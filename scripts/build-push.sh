#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-latest}"
PROJECT="${2:-proquip-dev}"

REGISTRY=$(oc get route default-route -n openshift-image-registry -o jsonpath='{.spec.host}')
TOKEN=$(oc whoami -t)

echo "Registry: ${REGISTRY}"
echo "Project:  ${PROJECT}"
echo "Tag:      ${TAG}"
echo ""

podman login "${REGISTRY}" --username unused --password "${TOKEN}" --tls-verify=false

cd "$(dirname "$0")/../proquip"

for svc in wildfly nginx; do
  IMAGE="${REGISTRY}/${PROJECT}/proquip-${svc}:${TAG}"
  echo "Building ${IMAGE}..."
  podman build -f "docker/${svc}/Dockerfile" -t "${IMAGE}" .
  podman push "${IMAGE}" --tls-verify=false
  echo "Pushed ${IMAGE}"
  echo ""
done

IMAGE="${REGISTRY}/${PROJECT}/proquip-keycloak:${TAG}"
echo "Building ${IMAGE}..."
podman build -f "docker/keycloak/Dockerfile" -t "${IMAGE}" docker/keycloak/
podman push "${IMAGE}" --tls-verify=false
echo "Pushed ${IMAGE}"
echo ""

echo "All images pushed with tag: ${TAG}"
oc get imagestream -n "${PROJECT}"
