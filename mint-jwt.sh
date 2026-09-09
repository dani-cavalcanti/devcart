#!/usr/bin/env bash
# Gera um JWT HS256 assinado com o JWT_SECRET do .env, para chamar o api-gateway.
# Uso:   ./mint-jwt.sh [sub] [horas]
#        export TOKEN=$(./mint-jwt.sh)
#        curl http://localhost:8080/api/catalogo/produtos -H "Authorization: Bearer $TOKEN"
set -euo pipefail
cd "$(dirname "$0")"
[ -f .env ] || { echo "crie o .env (cp .env.example .env e defina JWT_SECRET)" >&2; exit 1; }
SECRET=$(grep '^JWT_SECRET=' .env | cut -d= -f2-)
SUB=${1:-user-123}
HOURS=${2:-24}
python3 - "$SECRET" "$SUB" "$HOURS" <<'EOF'
import hmac, hashlib, base64, json, time, sys
secret, sub, hours = sys.argv[1], sys.argv[2], int(sys.argv[3])
b = lambda x: base64.urlsafe_b64encode(x).rstrip(b'=')
h = b(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(',', ':')).encode())
p = b(json.dumps({"sub": sub, "roles": ["USER"],
                  "iat": int(time.time()), "exp": int(time.time()) + hours * 3600},
                 separators=(',', ':')).encode())
s = b(hmac.new(secret.encode(), h + b'.' + p, hashlib.sha256).digest())
print((h + b'.' + p + b'.' + s).decode())
EOF
