"""Explicit, one-time AI account provisioning; never run as a database migration.

Requires Python 3.8+, PyMySQL and bcrypt. Supply MYSQL_HOST, MYSQL_PORT,
MYSQL_DATABASE, MYSQL_USERNAME, MYSQL_PASSWORD in the process environment.
Run with --user-id <configured publisher ID>; this only checks availability.
Add --create to insert a new USER account and print its random initial password.
Existing usernames/IDs/emails always cause refusal; no password reset/upsert.
"""
import argparse
import json
import os
import secrets
import sys

import bcrypt
import pymysql


def provision(connection, user_id, username, create=False):
    if not 0 < user_id < 2 ** 63:
        raise ValueError("user-id must be a positive signed BIGINT")
    if not username.isascii() or not username.replace("_", "").isalnum() or len(username) > 40:
        raise ValueError("username must contain 1-40 ASCII letters, digits or underscores")
    email = username + "@system.local"
    password = "Aa1!" + secrets.token_urlsafe(24)
    password_hash = bcrypt.hashpw(password.encode(), bcrypt.gensalt(rounds=12)).decode()
    try:
        connection.begin()
        with connection.cursor() as cursor:
            cursor.execute("SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES "
                           "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN "
                           "('users','user_profiles','roles','user_roles')")
            engines = cursor.fetchall()
            if len(engines) != 4 or any(row["ENGINE"].upper() != "INNODB" for row in engines):
                raise ValueError("All four account tables must exist and use InnoDB")
            cursor.execute("SELECT id FROM users WHERE id=%s OR username=%s OR email=%s FOR UPDATE",
                           (user_id, username, email))
            if cursor.fetchone():
                raise ValueError("Account ID, username or email already exists; nothing changed")
            cursor.execute("SELECT id FROM roles WHERE role_code='USER' AND status=1 FOR UPDATE")
            roles = cursor.fetchall()
            if len(roles) != 1:
                raise ValueError("Exactly one active USER role is required")
            for table in ("user_profiles", "user_roles"):
                cursor.execute("SELECT id FROM " + table + " WHERE user_id=%s FOR UPDATE", (user_id,))
                if cursor.fetchone():
                    raise ValueError("Orphan profile or roles exist for requested ID; nothing changed")
            if not create:
                connection.rollback()
                return {"created": False, "available": True, "id": str(user_id), "username": username}
            cursor.execute("INSERT INTO users (id,username,nickname,email,password_hash,avatar_url,gender,status,created_at,updated_at) "
                           "VALUES (%s,%s,%s,%s,%s,%s,0,1,NOW(),NOW())",
                           (user_id, username, "Matu AI", email, password_hash,
                            "https://your-bucket.oss-cn-hangzhou.aliyuncs.com/defaulthead.png"))
            cursor.execute("INSERT INTO user_profiles (id,user_id,activity_level,school_verified,company_verified,title_verified,"
                           "work_years,view_count,follower_count,following_count,is_vip,vip_level,vip_days_remaining) "
                           "VALUES (%s,%s,0,0,0,0,0,0,0,0,0,0,0)", (secrets.randbelow(2 ** 63 - 1) + 1, user_id))
            cursor.execute("INSERT INTO user_roles (id,user_id,role_id,created_at) VALUES (%s,%s,%s,NOW())",
                           (secrets.randbelow(2 ** 63 - 1) + 1, user_id, roles[0]["id"]))
            cursor.execute("SELECT password_hash FROM users WHERE id=%s", (user_id,))
            if not bcrypt.checkpw(password.encode(), cursor.fetchone()["password_hash"].encode()):
                raise ValueError("Password verification failed")
            cursor.execute("SELECT r.role_code FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=%s", (user_id,))
            if [row["role_code"] for row in cursor.fetchall()] != ["USER"]:
                raise ValueError("Role verification failed")
        connection.commit()
        return {"created": True, "id": str(user_id), "username": username,
                "initial_password": password, "roles": ["USER"], "bcrypt_verified": True}
    except Exception:
        connection.rollback()
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--user-id", required=True, type=int)
    parser.add_argument("--username", default="matu_ai")
    parser.add_argument("--create", action="store_true", help="explicitly authorize creation (otherwise read-only check)")
    args = parser.parse_args()
    required = ("MYSQL_HOST", "MYSQL_DATABASE", "MYSQL_USERNAME", "MYSQL_PASSWORD")
    if any(not os.environ.get(name) for name in required):
        parser.error("Required environment: " + ", ".join(required))
    connection = None
    try:
        connection = pymysql.connect(host=os.environ["MYSQL_HOST"], port=int(os.environ.get("MYSQL_PORT", "3306")),
                                     user=os.environ["MYSQL_USERNAME"], password=os.environ["MYSQL_PASSWORD"],
                                     database=os.environ["MYSQL_DATABASE"], charset="utf8mb4", autocommit=False,
                                     connect_timeout=5, read_timeout=10, write_timeout=10,
                                     cursorclass=pymysql.cursors.DictCursor)
        print(json.dumps(provision(connection, args.user_id, args.username, args.create), ensure_ascii=False))
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        return 1
    except pymysql.MySQLError as exc:
        # Do not echo connection credentials, SQL or server-provided data.
        print("Database operation failed (error code %s); no credentials are displayed" % exc.args[0], file=sys.stderr)
        return 1
    finally:
        if connection:
            connection.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
