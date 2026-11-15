# Load Tests (k6)

## Install k6

```bash
# Mac
brew install k6

# Windows
choco install k6
# or: winget install k6

# Linux
sudo snap install k6
```

## Run

```bash
# Smoke (1 VU, 10s) - proves the stack is alive
k6 run loadtests/k6/smoke.js

# Auth load (50 VUs) - hammers /api/auth/token
LOAD_TEST_KEY=loadtest_dev_bypass_key k6 run loadtests/k6/auth-load.js

# Upload load (10 VUs, 30s)
LOAD_TEST_KEY=loadtest_dev_bypass_key k6 run loadtests/k6/upload-load.js
```

## What to watch

- k6 prints pass/fail thresholds at the end
- Open http://localhost:8080/actuator/health - gateway status
- `docker stats` - CPU / memory usage
