# Contributing to BloodMagic Teams

Thank you for your interest in contributing to BloodMagic Teams!

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Set up the development environment:
   ```bash
   ./gradlew genEclipseRuns  # For Eclipse
   ./gradlew genIntellijRuns # For IntelliJ IDEA
   ```
4. Import the project into your IDE

## Development Guidelines

### Code Style

- Use 4 spaces for indentation (no tabs)
- Follow standard Java naming conventions
- Keep lines under 120 characters where practical
- Add Javadoc comments for public APIs

### Commit Messages

- Use clear, descriptive commit messages
- Reference issue numbers where applicable
- Format: `type: description` (e.g., `fix: resolve team binding crash`)

### Pull Requests

1. Create a feature branch from `main`
2. Make your changes with clear, focused commits
3. Test your changes in-game
4. Submit a PR with a clear description of changes
5. Respond to review feedback

## Project Structure

```
src/main/java/dev/ftb/bloodmagicteams/
├── BloodMagicTeams.java      # Main mod class
├── config/                    # Configuration
├── data/                      # Data storage (player preferences)
├── events/                    # Event handlers
├── integration/               # FTB Teams integration (soft dependency)
├── network/                   # Network packets
├── recipe/                    # Recipe handling
└── ui/                        # FTBLib UI screens
```

## Testing

- Test with both BloodMagic and FTB Teams installed
- Verify team binding works correctly
- Test unbinding recipes in alchemy arrays
- Check edge cases (leaving teams, offline players)

## Questions?

Open an issue on GitHub or reach out to the FTB Team.
