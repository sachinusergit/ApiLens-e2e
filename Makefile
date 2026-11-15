.PHONY: help up down logs ps test zip clean

help:
	@echo "Targets:"
	@echo "  make up     - start everything (DBs + services)"
	@echo "  make down   - stop all containers"
	@echo "  make logs   - follow logs"
	@echo "  make ps     - show container status"
	@echo "  make test   - run all tests (backend + microservices-tests)"
	@echo "  make zip    - build apilens-mvp-<timestamp>.zip"
	@echo "  make clean  - stop + remove volumes (DESTROYS DATA)"

up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

ps:
	docker compose ps

test:
	cd microservices-tests && mvn test

zip:
	./scripts/make-zip.sh

clean:
	docker compose down -v
