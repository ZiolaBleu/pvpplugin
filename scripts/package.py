"""Build a source + plugin JAR ZIP, excluding caches and private data."""
import hashlib
from pathlib import Path
import zipfile

root = Path(__file__).resolve().parents[1]
output = root.parent / 'pvp-tournament.zip'
files = []
for name in ('src', 'server', 'docs', 'scripts', 'gradle', '.github', 'third-party'):
    for path in (root / name).rglob('*'):
        if path.is_file() and '__pycache__' not in path.parts and path.suffix != '.pyc':
            files.append((path, path.relative_to(root)))
for name in ('build.gradle', 'settings.gradle', 'runelite-plugin.properties', 'gradlew',
             'gradlew.bat', '.gitignore', 'README.md', 'LICENSE', 'PRIVACY.md', 'THIRD_PARTY.md'):
    path = root / name
    if path.exists():
        files.append((path, Path(name)))
jar = root / 'build/libs/pvp-tournament-1.0.0.jar'
if jar.exists():
    files.append((jar, Path('dist') / jar.name))
preview = root / 'build/bracket-preview.png'
if preview.exists():
    files.append((preview, Path('docs/bracket-preview.png')))
with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
    for path, relative in sorted(files, key=lambda item: str(item[1])):
        archive.write(path, 'pvp-tournament/' + relative.as_posix())
with zipfile.ZipFile(output) as archive:
    assert archive.testzip() is None
sha = hashlib.sha256(output.read_bytes()).hexdigest()
output.with_suffix('.zip.sha256').write_text(f'{sha}  {output.name}\n', encoding='ascii')
print(f'{output}\n{len(files)} files; {output.stat().st_size:,} bytes\nSHA256 {sha}')
