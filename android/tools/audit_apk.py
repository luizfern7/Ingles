#!/usr/bin/env python3
"""Checks structural properties of the distributed APK. This is NOT a malware scanner."""
import argparse,hashlib,json,re,subprocess,xml.etree.ElementTree as E,zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('apk',type=Path);p.add_argument('--sdk',type=Path,required=True);p.add_argument('--previous',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
a.output.mkdir(parents=True,exist_ok=True)
manifest=subprocess.check_output([str(a.sdk/'cmdline-tools/latest/bin/apkanalyzer'),'manifest','print',str(a.apk)],text=True)
(a.output/'manifest-release.xml').write_text(manifest)
root=E.fromstring(manifest);ns='{http://schemas.android.com/apk/res/android}'
app=root.find('application');assert app is not None
assert root.get('package')=='br.com.desbloqueia'
assert app.get(ns+'debuggable','false')=='false','Distributed APK must not enable debugging'
assert app.get(ns+'allowBackup')=='false'
permissions=[x.get(ns+'name') for x in root.findall('uses-permission')]
allowed={'android.permission.POST_NOTIFICATIONS','android.permission.VIBRATE','br.com.desbloqueia.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'}
overlay=int(root.get(ns+'versionCode','0'))>=3
if overlay: allowed.update({'android.permission.SYSTEM_ALERT_WINDOW','android.permission.FOREGROUND_SERVICE','android.permission.FOREGROUND_SERVICE_SPECIAL_USE'})
assert set(permissions)==allowed,permissions
activities=[x.get(ns+'name') for x in app.findall('activity')]
assert activities==['br.com.desbloqueia.MainActivity'],activities
services=[x.get(ns+'name') for x in app.findall('service')]
expected_services={'androidx.room.MultiInstanceInvalidationService'}
if overlay: expected_services.add('br.com.desbloqueia.unlock.UnlockStudyService')
assert set(services)==expected_services,services
if overlay:
    service=next(x for x in app.findall('service') if x.get(ns+'name')=='br.com.desbloqueia.unlock.UnlockStudyService')
    assert service.get(ns+'foregroundServiceType') in {'specialUse','0x40000000'}
    assert any(x.get(ns+'name')=='android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE' for x in service.findall('property'))
assert all(x.get(ns+'exported')=='false' for x in app.findall('service'))
assert all(x.get(ns+'exported')=='false' for x in app.findall('provider'))
assert all(x.get(ns+'permission')=='android.permission.DUMP' for x in app.findall('receiver') if x.get(ns+'exported')=='true')
assert not app.findall('activity-alias')
with zipfile.ZipFile(a.apk) as z:
    scripts=[n for n in z.namelist() if n.lower().endswith(('.exe','.dll','.js','.html','.vbs','.ps1','.cmd','.bat','.py','.sh','.apk','.jar'))]
    assert not scripts,scripts
    assert set(n for n in z.namelist() if n.startswith('assets/'))=={'assets/questions.json','assets/dexopt/baseline.prof','assets/dexopt/baseline.profm'}
    assert len(json.loads(z.read('assets/questions.json'))['questions'])==25
    natives=[n for n in z.namelist() if n.startswith('lib/')]
    assert all(Path(n).name in {'libandroidx.graphics.path.so','libdatastore_shared_counter.so'} for n in natives)
apksigner=a.sdk/'build-tools/34.0.0/apksigner'
def certificate(apk):
    output=subprocess.check_output([str(apksigner),'verify','--verbose','--print-certs',str(apk)],text=True)
    digest=re.search(r'Signer #1 certificate SHA-256 digest: (\w+)',output)
    assert digest,'Signer digest missing'
    return output,digest.group(1)
signature,cert=certificate(a.apk);(a.output/'signature-release.txt').write_text(signature)
if a.previous:
    _,original=certificate(a.previous);assert cert==original,'Update signing certificate changed'
report=dict(file=a.apk.name,sha256=hashlib.sha256(a.apk.read_bytes()).hexdigest(),version=root.get(ns+'versionName'),version_code=root.get(ns+'versionCode'),debuggable=False,permissions=permissions,activities=activities,internal_services=services,native_libraries=natives,certificate_sha256=cert,previous_certificate_matches=True if a.previous else None,malware_scan_performed_by_this_script=False)
(a.output/'audit.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
