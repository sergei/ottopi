VERSION=v`python3 -c  "import yaml; print(yaml.safe_load(open('navcomputer/openapi/ottopi.yaml') )['info']['version'])"`

echo Building version ${VERSION}

./make-pkg.sh

ASSET_NAME=update/otto-pi-${VERSION}.tgz
mv update/otto-pi-update.tgz ${ASSET_NAME}
echo Created ${ASSET_NAME}

gh release create ${VERSION} -t "Version ${VERSION}" -n  "Version ${VERSION}" ${ASSET_NAME}
