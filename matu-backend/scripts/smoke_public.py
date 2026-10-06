"""Read-only smoke checks against an already running gateway (no login or data writes)."""
import argparse
import json
from urllib.request import urlopen, Request
from urllib.error import HTTPError

PUBLIC = [
    '/posts?pageNum=1&pageSize=1', '/checks?pageNum=1&pageSize=1',
    '/qa/questions?pageNum=1&pageSize=1', '/courses?pageNum=1&pageSize=1',
    '/interview/questions?pageNum=1&pageSize=1',
    '/oj/classes/problems?pageNum=1&pageSize=1', '/announcements?pageNum=1&pageSize=1',
]
PRIVATE = ['/auth/me', '/feedbacks/mine', '/admin/info/feedbacks', '/courses/1/progress/mine']


def get(base, path):
    try:
        response = urlopen(Request(base.rstrip('/') + path, headers={'Accept': 'application/json'}), timeout=10)
    except HTTPError as error:
        response = error
    with response:
        return response.code, json.load(response)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:8080')
    args = parser.parse_args()
    failures = []
    for path in PUBLIC + PRIVATE:
        try:
            status, body = get(args.base_url, path)
            code = body.get('code')
            if path in PUBLIC:
                ok = status == 200 and code in (0, 200)
            else:
                ok = status in (401, 403) or code in (401, 403) or (
                    status == 400 and any(hint in str(body.get('message', '')) for hint in ('未登录', '登录已失效', '请重新登录')))
            print(('PASS' if ok else 'FAIL') + f' {path.split("?")[0]} HTTP={status} code={code}')
            if not ok:
                failures.append(path)
        except Exception as error:
            failures.append(path)
            print(f'FAIL {path.split("?")[0]} {type(error).__name__}')
    return 1 if failures else 0


if __name__ == '__main__':
    raise SystemExit(main())
