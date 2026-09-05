import sys

with open('androidApp/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace('versionCode = 6', 'versionCode = 7')
content = content.replace('versionName = "1.8"', 'versionName = "2.0"')

with open('androidApp/build.gradle.kts', 'w') as f:
    f.write(content)

