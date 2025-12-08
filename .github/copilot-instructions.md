# Copilot Instructions for Strudel

## Project Overview

Strudel is a live coding pattern language for the web, a port of TidalCycles to JavaScript. It enables algorithmic pattern generation for music and audio in the browser.

- **License**: AGPL-3.0-or-later (all code must be compatible with this license)
- **Homepage**: https://strudel.cc
- **Main Repository**: https://codeberg.org/uzu/strudel

## Project Architecture

### Monorepo Structure

This is a **pnpm workspace monorepo** managed with **Lerna**:
- Individual packages are in `packages/` directory
- Each package has independent versioning
- Use workspace protocol for internal dependencies (`workspace:*`)
- Never use npm or yarn - always use `pnpm`

### Key Directories

- `packages/` - Individual npm packages (core, transpiler, tonal, mini, etc.)
- `website/` - Main Strudel web application (Astro-based)
- `examples/` - Example implementations and demos
- `test/` - Root-level integration tests
- `docs/` - Documentation and papers

## Code Style & Standards

### JavaScript/ESM

- **Module System**: ES Modules (`.mjs` file extension)
- **Module Type**: Always use `"type": "module"` in package.json
- **No TypeScript**: This project uses vanilla JavaScript with JSDoc for typing
- **Formatting**: Use Prettier (run `pnpm codeformat` before committing)
- **Linting**: ESLint with project config (run `pnpm lint`)

### File Naming

- Use **kebab-case** for file names
- Use `.mjs` extension for JavaScript modules
- Use `.test.mjs` for test files
- Use `.bench.mjs` for benchmark files

### Code Formatting

- Run `pnpm codeformat` to format all files
- Run `pnpm format-check` to verify formatting
- Prettier configuration is at project root
- Always format before committing

### Linting

- ESLint config is in `eslint.config.mjs`
- Run `pnpm lint` to check
- Fix linting issues before submitting PRs

## Documentation Standards

### JSDoc Comments

**CRITICAL**: All exported functions, classes, and significant methods must have JSDoc comments.

- The project generates documentation from JSDoc using `pnpm jsdoc-json`
- JSDoc is used for API reference generation at https://strudel.cc/learn/code/
- Missing JSDoc is tracked in `undocumented.json` (generated via `pnpm report-undocumented`)

**Example JSDoc format**:

```javascript
/**
 * Creates a pattern from the given value.
 * @name pure
 * @memberof Pattern
 * @param {any} value - The value to create a pattern from
 * @returns {Pattern} A new pattern containing the value
 * @example
 * pure(4).fast(2)
 * // returns a pattern that repeats 4 twice per cycle
 */
export const pure = (value) => new Pattern((state) => [new Hap(state.span, state.span, value)]);
```

**Required JSDoc tags**:
- `@name` - Function/method name
- `@memberof` - Parent class/namespace (if applicable)
- `@param` - All parameters with types and descriptions
- `@returns` - Return value with type and description
- `@example` - Usage examples (highly encouraged)
- `@synonyms` - Alternative names/aliases (if applicable)

### Code Comments

- Write clear, concise comments for complex logic
- Explain *why* something is done, not just *what*
- Use inline comments sparingly - prefer self-documenting code
- Document any workarounds or non-obvious solutions

## Testing

### Test Framework

- **Vitest** is used for all testing
- Test files use `.test.mjs` extension
- Tests are in `test/` folders within each package
- Root-level integration tests are in `test/`

### Running Tests

```bash
pnpm test          # Run all tests
pnpm test-ui       # Run tests with UI
pnpm test-coverage # Run tests with coverage
pnpm bench         # Run benchmarks
pnpm snapshot      # Update snapshots
```

### Writing Tests

- Write tests for all new functions
- Use descriptive test names with `describe()` and `it()`
- Include edge cases and error conditions
- Update snapshots when intentionally changing output

**Example test structure**:

```javascript
import { describe, it, expect } from 'vitest';
import { myFunction } from '../myFunction.mjs';

describe('myFunction', () => {
  it('should handle basic case', () => {
    expect(myFunction('input')).toBe('expected');
  });

  it('should handle edge case', () => {
    expect(myFunction('')).toBe('');
  });
});
```

## Development Workflow

### Setup Commands

```bash
pnpm i              # Install dependencies
pnpm dev            # Start development server
pnpm build          # Build for production
pnpm check          # Run format-check, lint, and tests
```

### Before Committing

1. Run `pnpm codeformat` to format code
2. Run `pnpm lint` to check for linting errors
3. Run `pnpm test` to ensure tests pass
4. Or simply run `pnpm check` to do all of the above

### Commit Messages

- Always write commit messages in **past tense**
- Use imperative mood descriptively: "fixed bug", "added feature", "updated config"
- Be specific and concise about what changed
- Examples:
  - ✅ "fixed github deployment issue, updated site URL"
  - ✅ "removed stray list.json reference from swatch page"
  - ❌ "fix github deployment issue" (present tense)
  - ❌ "fixing bugs" (present continuous)

### Adding New Packages

- Create package in `packages/` directory
- Include `package.json` with:
  - `"type": "module"`
  - `"main": "index.mjs"`
  - Author: `Alex McLean <alex@slab.org> (https://slab.org)`
  - License: `AGPL-3.0-or-later`
  - Repository URL pointing to codeberg
- Add to workspace via pnpm

## Package Dependencies

- Use `workspace:*` for internal Strudel packages
- Minimize external dependencies
- Check license compatibility (AGPL-3.0-or-later)
- Document why dependencies are needed

## Pattern System (Core Concepts)

When working with pattern-related code:

- Patterns are time-based and cyclic
- Use `Hap` (happening) for discrete events
- `TimeSpan` represents time intervals as fractions
- Functions are typically chainable/composable
- Prefer pure functional approaches
- Document pattern transformations clearly

## Common Patterns to Follow

### Function Exports

```javascript
// Prefer named exports
export const myFunction = () => { /* ... */ };

// For pattern methods
Pattern.prototype.myMethod = function() { /* ... */ };
```

### Error Handling

- Throw descriptive errors with context
- Validate inputs for public APIs
- Use console warnings for deprecations

### Performance

- Patterns can be computationally intensive
- Consider performance for hot paths
- Use benchmarks (`*.bench.mjs`) for optimization work

## Communication

- **Discord**: #strudel channel in Tidal Discord
- **Forum**: https://club.tidalcycles.org/
- **Issues**: https://codeberg.org/uzu/strudel/issues

## Additional Notes

- This is a **personal fork** - always sync with upstream Codeberg repo
- The project is moving from GitHub to Codeberg
- Website runs on Astro framework
- Audio generation uses Web Audio API
- REPL (website) is the primary user interface

## When Making Changes

1. Understand the pattern system and Strudel's musical DSL
2. Maintain backward compatibility where possible
3. Add examples for new features
4. Update documentation in `website/src/` if adding user-facing features
5. Consider impact on live coding performance
6. Test in the browser - this is a web-first project

---

**Remember**: Strudel is about making music creation accessible and fun. Keep the API intuitive and well-documented for live coders and musicians!



For pattern generation, use documentation from strudel site itself

https://strudel.cc/workshop/first-sounds/
https://strudel.cc/workshop/first-notes/
https://strudel.cc/recipes/recipes/

