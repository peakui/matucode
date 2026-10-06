"""Schema-only export and isolated bootstrap verification. Requires pymysql.

Reads MYSQL_* from the environment or workspace .env; never exports business rows.
Verification creates temporary matu_verify_* databases and removes only those it created.
"""
import argparse
import os
from pathlib import Path
import re
import uuid

import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]


def connect():
    config = {}
    env_file = ROOT.parent / '.env'
    if env_file.exists():
        for line in env_file.read_text(encoding='utf-8-sig').splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                key, value = line.split('=', 1)
                config[key.strip()] = value.strip().strip('\"\'')
    config.update(os.environ)
    return pymysql.connect(host=config.get('MYSQL_HOST', '127.0.0.1'),
                           port=int(config.get('MYSQL_PORT', '3306')),
                           user=config.get('MYSQL_USERNAME', 'root'),
                           password=config.get('MYSQL_PASSWORD', ''),
                           charset='utf8mb4', autocommit=True, connect_timeout=8,
                           client_flag=CLIENT.MULTI_STATEMENTS)


def execute_script(cursor, sql):
    cursor.execute(sql)
    while cursor.nextset():
        pass


def export_schema(conn):
    target = ROOT / 'database' / 'bootstrap'
    target.mkdir(parents=True, exist_ok=True)
    with conn.cursor() as cursor:
        for service in sorted((ROOT / 'service').glob('service-*')):
            if service.name in ('service-mcp', 'service-search'):
                continue
            db = 'matu_' + service.name[len('service-'):]
            cursor.execute(f'SHOW FULL TABLES FROM `{db}` WHERE Table_type = "BASE TABLE"')
            tables = [row[0] for row in cursor.fetchall() if row[0] != 'flyway_schema_history']
            statements = ['-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.\nSET NAMES utf8mb4;']
            definitions = {}
            for table in sorted(tables):
                cursor.execute(f'SHOW CREATE TABLE `{db}`.`{table}`')
                ddl = cursor.fetchone()[1]
                ddl = re.sub(r' AUTO_INCREMENT=\d+', '', ddl)
                definitions[table] = ddl
            emitted = set()
            while definitions:
                ready = [table for table, ddl in definitions.items()
                         if set(re.findall(r'REFERENCES `(\w+)`', ddl)) - {table} <= emitted]
                if not ready:
                    raise RuntimeError(f'{db}: cyclic or external foreign keys require manual bootstrap ordering')
                for table in sorted(ready):
                    statements.append(definitions.pop(table) + ';')
                    emitted.add(table)
            (target / f'{db}.sql').write_text('\n\n'.join(statements) + '\n', encoding='utf-8')
            print(f'{db}: exported {len(tables)} table definitions (no rows)')


def verify(conn):
    with conn.cursor() as cursor:
        for schema in sorted((ROOT / 'database' / 'bootstrap').glob('matu_*.sql')):
            temp = 'matu_verify_' + uuid.uuid4().hex
            cursor.execute(f'CREATE DATABASE `{temp}` CHARACTER SET utf8mb4')
            try:
                cursor.execute(f'USE `{temp}`')
                execute_script(cursor, schema.read_text(encoding='utf-8'))
                seed = ROOT / 'database/seeds' / schema.name
                if seed.exists():
                    execute_script(cursor, seed.read_text(encoding='utf-8'))
                service = ROOT / 'service' / schema.stem.replace('matu_', 'service-', 1)
                location = 'migration-mysql' if schema.stem == 'matu_ai' else 'migration'
                migrations = sorted((service / 'src/main/resources/db' / location).glob('V*__*.sql'),
                                    key=lambda f: tuple(map(int, f.name.split('__')[0][1:].replace('_', '.').split('.'))))
                for migration in migrations:
                    execute_script(cursor, migration.read_text(encoding='utf-8-sig'))
                # Every mapped persisted field must exist in the bootstrapped schema.
                checked = 0
                for entity in service.glob('src/main/java/**/*.java'):
                    source = entity.read_text(encoding='utf-8-sig')
                    match = re.search(r'@TableName\("(\w+)"\)', source)
                    if not match:
                        continue
                    table = match.group(1)
                    cursor.execute(f'SHOW COLUMNS FROM `{table}`')
                    columns = {row[0] for row in cursor.fetchall()}
                    source = re.sub(r'@TableField\(\s*exist\s*=\s*false\s*\)\s*private[^;]+;', '', source)
                    fields = re.findall(r'private\s+[\w<>?, ]+\s+(\w+)\s*;', source)
                    expected = {re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower() for name in fields}
                    missing = expected - columns
                    if missing:
                        raise RuntimeError(f'{schema.stem}.{table}: missing columns {sorted(missing)}')
                    checked += 1
                print(f'{schema.stem}: bootstrap + {len(migrations)} migrations + {checked} entity mappings verified')
            finally:
                # temp is generated above, never derived from connection configuration or user input.
                if not re.fullmatch(r'matu_verify_[0-9a-f]{32}', temp):
                    raise RuntimeError('Invalid verification database name')
                cursor.execute(f'DROP DATABASE `{temp}`')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--export', action='store_true', help='Refresh schema-only bootstrap files')
    parser.add_argument('--verify', action='store_true', help='Test bootstrap/migrations in disposable databases')
    args = parser.parse_args()
    if not args.export and not args.verify:
        parser.error('Choose --export or --verify')
    connection = connect()
    try:
        if args.export:
            export_schema(connection)
        if args.verify:
            verify(connection)
    finally:
        connection.close()
