"""Run the real-MySQL workflow checks after a successful reactor mvn test.
Uses test classpaths recorded by Surefire; credentials only travel via child environment.
"""
import argparse
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--java', default=str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else 'java')
args = parser.parse_args()
classpath = []
for module in ('service-course', 'service-info'):
    reports = sorted((ROOT / 'service' / module / 'target/surefire-reports').glob('TEST-*.xml'))
    if not reports:
        raise SystemExit('Run mvn test successfully before integration checks')
    tree = ET.parse(reports[0])
    prop = tree.find(".//property[@name='java.class.path']")
    if prop is None:
        raise SystemExit('Surefire classpath missing')
    for item in prop.attrib['value'].split(os.pathsep):
        if item and item not in classpath:
            classpath.append(item)
env = {}
env_file = ROOT.parent / '.env'
if env_file.exists():
    for line in env_file.read_text(encoding='utf-8-sig').splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            key, value = line.split('=', 1)
            env[key.strip()] = value.strip().strip('\"\'')
env.update(os.environ)
result = subprocess.run([args.java, '-Dfile.encoding=UTF-8', '-cp', os.pathsep.join(classpath),
                         str(ROOT / 'scripts/ClosureIntegration.java'), str(ROOT)], cwd=str(ROOT), env=env)
raise SystemExit(result.returncode)
