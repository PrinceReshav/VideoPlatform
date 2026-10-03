# Contributing to Video Platform

Thank you for your interest in contributing to Video Platform! This document provides guidelines for contributing to the project.

## Code of Conduct

This project follows our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/your-org/video-platform/issues)
2. Create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots (if applicable)
   - Environment details (OS, browser, version)

### Suggesting Features

1. Check existing [Feature Requests](https://github.com/your-org/video-platform/issues?q=is%3Aissue+label%3Aenhancement)
2. Create a new issue with:
   - Clear title and description
   - Use case and motivation
   - Proposed solution (if any)
   - Alternatives considered

### Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Ensure tests pass: `make test`
5. Run linting: `make lint`
6. Commit with conventional commits: `git commit -m "feat: add amazing feature"`
7. Push to your fork
8. Create a Pull Request

## Development Setup

### Prerequisites

- Docker & Docker Compose
- Java 21+
- Node.js 20+
- Maven 3.9+
- Git

### Quick Start

```bash
# Clone the repository
git clone https://github.com/your-org/video-platform.git
cd video-platform

# Copy environment template
cp .env.example .env

# Start infrastructure
make up

# Verify health
make health

# Run tests
make test
```

## Coding Standards

### Backend (Java/Spring Boot)

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use Spring Boot best practices
- Write unit tests for new code (>80% coverage)
- Use meaningful variable/method names
- Document public APIs with JavaDoc

### Frontend (React/TypeScript)

- Follow [Airbnb React Style Guide](https://github.com/airbnb/javascript/tree/master/react)
- Use TypeScript strict mode
- Use functional components with hooks
- Write tests with React Testing Library
- Use CSS Modules for styling

### Mobile (React Native/Expo)

- Follow React Native best practices
- Use TypeScript
- Test on iOS and Android
- Handle platform differences gracefully

### Database

- Use Flyway for migrations
- Follow naming conventions: `V{version}__{description}.sql`
- Use UUIDs for primary keys
- Add appropriate indexes
- Use soft deletes where appropriate

## Commit Message Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

Examples:
```
feat(auth): add password reset functionality
fix(video): resolve HLS playback issue on Safari
docs(api): update video upload API documentation
test(upload): add integration tests for chunked upload
```

## Branch Naming

- Feature: `feature/description`
- Bug fix: `fix/description`
- Hotfix: `hotfix/description`
- Release: `release/version`
- Docs: `docs/description`

## Testing Requirements

- All new code must have tests
- Unit tests: >80% coverage
- Integration tests for critical paths
- E2E tests for user-facing features
- Run tests before committing: `make test`

## Code Review Process

1. All PRs require at least 1 approval
2. CI must pass (lint, typecheck, tests)
3. Address all review comments
4. Squash commits before merge
5. Delete branch after merge

## Release Process

1. Create release branch: `release/v1.0.0`
2. Update version in `pom.xml` and `package.json`
3. Update CHANGELOG.md
4. Create PR to main
5. After merge, tag release: `git tag v1.0.0`
5. GitHub Actions builds and publishes Docker images

## Getting Help

- Check [Documentation](docs/)
- Search [Issues](https://github.com/your-org/video-platform/issues)
- Join [Discord Community](https://discord.gg/your-invite)
- Email: contributors@your-org.com

## Recognition

Contributors are recognized in:
- CONTRIBUTORS.md
- Release notes
- Project README

Thank you for contributing to Video Platform! 🎉