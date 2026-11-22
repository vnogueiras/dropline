#!/usr/bin/env bash
#
# doctor.sh: checks that every tool needed for Dropline is installed at a
# supported version. Prints one line per tool and exits non-zero if a
# REQUIRED tool is missing or too old.
#
# Usage:
#   ./scripts/doctor.sh          # required tools must pass; later-phase tools only warn
#   ./scripts/doctor.sh --all    # every tool is required (use before Phase 6/7)
#   ./scripts/doctor.sh --help
#
# Written for the bash 3.2 that ships with macOS (no associative arrays, no mapfile).

set -uo pipefail

STRICT_ALL=false
case "${1:-}" in
  --all) STRICT_ALL=true ;;
  -h|--help)
    sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
    exit 0 ;;
  "") ;;
  *) echo "Unknown option: $1 (try --help)" >&2; exit 2 ;;
esac

# ---------- output helpers ----------------------------------------------------
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
  RED=$'\033[31m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; BLUE=$'\033[34m'; DIM=$'\033[2m'; BOLD=$'\033[1m'; RESET=$'\033[0m'
else
  RED=""; GREEN=""; YELLOW=""; BLUE=""; DIM=""; BOLD=""; RESET=""
fi

FAILURES=0
WARNINGS=0

ok()      { printf "  ${GREEN}✔${RESET} %-16s %-12s ${DIM}%s${RESET}\n" "$1" "$2" "${3:-}"; }
fail()    { printf "  ${RED}✘${RESET} %-16s %-12s ${RED}%s${RESET}\n" "$1" "$2" "$3"; FAILURES=$((FAILURES + 1)); }
warn()    { printf "  ${YELLOW}!${RESET} %-16s %-12s ${YELLOW}%s${RESET}\n" "$1" "$2" "$3"; WARNINGS=$((WARNINGS + 1)); }
section() { printf "\n${BOLD}${BLUE}%s${RESET}\n" "$1"; }

# ---------- version helpers ---------------------------------------------------
# Extract the first x.y or x.y.z number from a command's output.
extract_version() {
  grep -Eo '[0-9]+(\.[0-9]+)+' | head -n 1
}

# version_ge A B → true if A >= B (numeric, dot-separated, pure bash).
version_ge() {
  local a="$1" b="$2" i ai bi
  local IFS=.
  # shellcheck disable=SC2206
  local av=($a) bv=($b)
  for i in 0 1 2 3; do
    ai="${av[$i]:-0}"; bi="${bv[$i]:-0}"
    ai="${ai%%[!0-9]*}"; bi="${bi%%[!0-9]*}"
    ai="${ai:-0}"; bi="${bi:-0}"
    if ((10#$ai > 10#$bi)); then return 0; fi
    if ((10#$ai < 10#$bi)); then return 1; fi
  done
  return 0
}

# check NAME MIN_VERSION LEVEL INSTALL_HINT VERSION_COMMAND...
#   LEVEL: required | later   ("later" = needed in a later phase; only warns unless --all)
check() {
  local name="$1" min="$2" level="$3" hint="$4"; shift 4
  local bin="$1"

  local report_missing=fail
  if [ "$level" = "later" ] && [ "$STRICT_ALL" = false ]; then report_missing=warn; fi

  if ! command -v "$bin" >/dev/null 2>&1; then
    $report_missing "$name" "missing" "install: $hint"
    return
  fi

  local raw version
  raw="$("$@" 2>&1)" || { $report_missing "$name" "broken" "'$*' failed: $(echo "$raw" | head -n 1)"; return; }
  version="$(printf '%s\n' "$raw" | extract_version)"

  if [ -z "$version" ]; then
    warn "$name" "unknown" "could not parse version from '$*'"
  elif version_ge "$version" "$min"; then
    ok "$name" "$version" "(>= $min)"
  else
    $report_missing "$name" "$version" "too old, need >= $min → $hint"
  fi
}

# ---------- locate repo root --------------------------------------------------
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OS="$(uname -s)"
ARCH="$(uname -m)"

printf "${BOLD}Dropline doctor${RESET}  ${DIM}(%s %s, repo: %s)${RESET}\n" "$OS" "$ARCH" "$REPO_ROOT"
[ "$STRICT_ALL" = true ] && printf "${DIM}Mode: --all (later-phase tools are required too)${RESET}\n"

# ---------- system ------------------------------------------------------------
section "System"
if [ "$OS" = "Darwin" ]; then
  if xcode-select -p >/dev/null 2>&1; then
    ok "xcode CLT" "installed" "$(xcode-select -p)"
  else
    fail "xcode CLT" "missing" "run: xcode-select --install"
  fi
  if [ "$ARCH" = "arm64" ]; then
    ok "cpu" "arm64" "Apple Silicon"
  else
    warn "cpu" "$ARCH" "running under Rosetta? Open a native (arm64) terminal"
  fi
  check "homebrew" "4.0" required "https://brew.sh" brew --version
fi
check "git" "2.39" required "brew install git" git --version

# ---------- java / maven ------------------------------------------------------
section "Backend (Java 21 + Maven)"
JAVA_REQUIRED_MAJOR=21
if command -v java >/dev/null 2>&1 && java -version >/dev/null 2>&1; then
  JAVA_RAW="$(java -version 2>&1)"
  JAVA_VERSION="$(printf '%s\n' "$JAVA_RAW" | extract_version)"
  # "1.8.0_x" style → major 8; "21.0.5" → major 21
  JAVA_MAJOR="${JAVA_VERSION%%.*}"
  [ "$JAVA_MAJOR" = "1" ] && JAVA_MAJOR="$(echo "$JAVA_VERSION" | cut -d. -f2)"
  JAVA_VENDOR="$(printf '%s\n' "$JAVA_RAW" | sed -n '2p' | sed 's/ (build.*//')"
  if [ "$JAVA_MAJOR" = "$JAVA_REQUIRED_MAJOR" ]; then
    ok "java" "$JAVA_VERSION" "$JAVA_VENDOR"
  elif [ "$JAVA_MAJOR" -gt "$JAVA_REQUIRED_MAJOR" ] 2>/dev/null; then
    warn "java" "$JAVA_VERSION" "project targets $JAVA_REQUIRED_MAJOR → sdk use java 21-tem (or cd into repo with .sdkmanrc)"
  else
    fail "java" "$JAVA_VERSION" "need Java $JAVA_REQUIRED_MAJOR → sdk install java 21-tem"
  fi
  JAVA_ARCH="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/os.arch/ {print $2; exit}')"
  if [ "$OS" = "Darwin" ] && [ "$ARCH" = "arm64" ] && [ "$JAVA_ARCH" != "aarch64" ]; then
    warn "java arch" "${JAVA_ARCH:-?}" "x86 JDK on Apple Silicon (slow, Rosetta); install the aarch64 build"
  elif [ -n "$JAVA_ARCH" ]; then
    ok "java arch" "$JAVA_ARCH" ""
  fi
  if [ -n "${JAVA_HOME:-}" ]; then
    ok "JAVA_HOME" "set" "$JAVA_HOME"
  else
    warn "JAVA_HOME" "unset" "IntelliJ/Maven may pick another JDK; SDKMAN sets it for you"
  fi
else
  fail "java" "missing" "install SDKMAN (https://sdkman.io) then: sdk install java 21-tem"
fi

if [ -n "${SDKMAN_DIR:-}" ] || [ -d "$HOME/.sdkman" ]; then
  ok "sdkman" "installed" "${SDKMAN_DIR:-$HOME/.sdkman}"
else
  warn "sdkman" "missing" "optional but recommended: curl -s https://get.sdkman.io | bash"
fi
if [ -f "$REPO_ROOT/.sdkmanrc" ]; then
  ok ".sdkmanrc" "present" "$(grep -E '^java=' "$REPO_ROOT/.sdkmanrc" || true)"
else
  warn ".sdkmanrc" "missing" "add: echo 'java=21.0.5-tem' > .sdkmanrc  (match your installed version)"
fi

if [ -x "$REPO_ROOT/backend/mvnw" ]; then
  MVNW_VERSION="$(cd "$REPO_ROOT/backend" && ./mvnw -v 2>/dev/null | extract_version)"
  ok "maven wrapper" "${MVNW_VERSION:-?}" "backend/mvnw"
else
  warn "maven wrapper" "missing" "created in P1-01: cd backend && mvn wrapper:wrapper"
fi
# lefthook's spotless hook currently calls a global `mvn`
check "maven (global)" "3.9" required "brew install maven  (only needed until hooks use ./mvnw)" mvn -v

# ---------- frontend ----------------------------------------------------------
section "Frontend (Node + Angular)"
check "node" "22.12" required "brew install fnm && fnm install --lts" node --version
check "npm" "10.0" required "comes with node" npm --version
if [ -f "$REPO_ROOT/.nvmrc" ] || [ -f "$REPO_ROOT/frontend/.nvmrc" ]; then
  ok ".nvmrc" "present" ""
else
  warn ".nvmrc" "missing" "add: node --version > .nvmrc"
fi

# ---------- containers --------------------------------------------------------
section "Containers"
check "docker" "24.0" required "brew install orbstack  (or Docker Desktop)" docker --version
if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    ok "docker daemon" "running" "context: $(docker context show 2>/dev/null || echo default)"
  else
    fail "docker daemon" "stopped" "start it: open -a OrbStack  (or open -a Docker)"
  fi
  check "docker compose" "2.20" required "included with OrbStack/Docker Desktop" docker compose version
fi

# ---------- git hooks / conventions -------------------------------------------
section "Git hooks"
check "lefthook" "1.5" required "brew install lefthook && lefthook install" lefthook version
if command -v commitlint >/dev/null 2>&1; then
  # Go commitlint uses `commitlint version`; the npm one uses `--version`
  CL_VERSION="$( { commitlint version 2>/dev/null || commitlint --version 2>/dev/null; } | extract_version)"
  ok "commitlint" "${CL_VERSION:-?}" "$(command -v commitlint)"
else
  fail "commitlint" "missing" "install: brew install conventionalcommit/tap/commitlint  (or go install)"
fi
if [ -f "$REPO_ROOT/.git/hooks/commit-msg" ] && grep -q lefthook "$REPO_ROOT/.git/hooks/commit-msg" 2>/dev/null; then
  ok "hooks installed" "yes" ".git/hooks → lefthook"
else
  fail "hooks installed" "no" "run: lefthook install"
fi

# ---------- later phases ------------------------------------------------------
section "Kubernetes & cloud (Phase 6–7)"
check "kubectl" "1.30" later "brew install kubectl" kubectl version --client
if command -v kind >/dev/null 2>&1; then
  check "kind" "0.20" later "brew install kind" kind version
elif command -v k3d >/dev/null 2>&1; then
  check "k3d" "5.6" later "brew install k3d" k3d version
else
  if [ "$STRICT_ALL" = true ]; then fail "kind/k3d" "missing" "brew install kind  (or k3d)"
  else warn "kind/k3d" "missing" "brew install kind  (or k3d)"; fi
fi
check "helm" "3.14" later "brew install helm" helm version --short
check "k9s" "0.30" later "brew install k9s" k9s version --short
check "terraform" "1.6" later "brew install tfenv && tfenv install latest" terraform version
check "gcloud" "450.0" later "brew install --cask google-cloud-sdk" gcloud --version
check "k6" "0.50" later "brew install k6" k6 version

# ---------- summary -----------------------------------------------------------
printf "\n"
if [ "$FAILURES" -gt 0 ]; then
  printf "${RED}${BOLD}✘ %d problem(s)${RESET}, %d warning(s). Fix the red lines above and run again.\n" "$FAILURES" "$WARNINGS"
  exit 1
elif [ "$WARNINGS" -gt 0 ]; then
  printf "${GREEN}${BOLD}✔ Ready${RESET} with %d warning(s) (yellow lines are optional or for later phases).\n" "$WARNINGS"
else
  printf "${GREEN}${BOLD}✔ All good.${RESET}\n"
fi
exit 0
