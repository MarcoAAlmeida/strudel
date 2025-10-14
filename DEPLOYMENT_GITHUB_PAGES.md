# Deploying Strudel to GitHub Pages

This guide explains how to deploy the Astro website (located in the `website` folder) to GitHub Pages. There are several deployment alternatives available.

## Prerequisites

- A GitHub account
- A fork of this repository (or your own copy)
- Node.js and pnpm installed locally (for local builds)

## Configuration Changes Required

Before deploying to GitHub Pages, you need to update the configuration in `website/astro.config.mjs`:

### For Repository Pages (username.github.io/repository-name)

If deploying to a repository page (e.g., `https://username.github.io/strudel`):

```js
const site = 'https://username.github.io';
const base = '/strudel'; // or '/repository-name'
```

### For User/Organization Pages (username.github.io)

If deploying to a user or organization page (e.g., `https://username.github.io`):

```js
const site = 'https://username.github.io';
const base = '/';
```

### CNAME File (Optional)

If using a custom domain, update `website/public/CNAME` to contain your custom domain:

```
yourdomain.com
```

For repository pages without a custom domain, you may want to remove or update this file.

## Deployment Alternatives

### Option 1: GitHub Actions (Recommended)

This is the most automated approach. GitHub Actions will automatically build and deploy your site whenever you push to the specified branch.

#### Steps:

1. **Create a GitHub Actions workflow file** at `.github/workflows/deploy-pages.yml`:

```yaml
name: Deploy to GitHub Pages

on:
  # Runs on pushes to main branch
  push:
    branches: [main]
  # Allows manual trigger from Actions tab
  workflow_dispatch:

# Sets permissions of the GITHUB_TOKEN to allow deployment to GitHub Pages
permissions:
  contents: read
  pages: write
  id-token: write

# Allow only one concurrent deployment
concurrency:
  group: "pages"
  cancel-in-progress: false

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: 20
      
      - name: Setup pnpm
        uses: pnpm/action-setup@v4
        with:
          version: 9.12.2
      
      - name: Install dependencies
        run: pnpm install
      
      - name: Build website
        run: |
          cd website
          pnpm run build
      
      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: ./website/dist

  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    needs: build
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

2. **Enable GitHub Pages in your repository settings:**
   - Go to your repository on GitHub
   - Navigate to **Settings** > **Pages**
   - Under "Source", select **GitHub Actions**

3. **Push your changes** to the main branch (or trigger the workflow manually from the Actions tab)

4. **Access your deployed site** at:
   - Repository pages: `https://username.github.io/repository-name`
   - User/Organization pages: `https://username.github.io`

### Option 2: Manual Build and Deploy

This approach gives you full control over when deployments happen.

#### Steps:

1. **Build the website locally:**

```bash
cd website
pnpm install
pnpm run build
```

This creates a `website/dist` folder with the static files.

2. **Deploy using GitHub CLI (gh):**

If you have the GitHub CLI installed:

```bash
# From the repository root
gh workflow run deploy-pages.yml
```

Or create a simple deployment workflow that you trigger manually.

3. **Alternative: Deploy using gh-pages package:**

Install the gh-pages package:

```bash
npm install -g gh-pages
```

Deploy the dist folder:

```bash
gh-pages -d website/dist -b gh-pages
```

Then configure GitHub Pages to use the `gh-pages` branch in your repository settings.

### Option 3: Deploy from a Branch

This method involves committing the built files to a specific branch.

#### Steps:

1. **Build the website:**

```bash
cd website
pnpm install
pnpm run build
```

2. **Create a separate branch for deployment:**

```bash
git checkout --orphan gh-pages
git rm -rf .
```

3. **Copy the built files:**

```bash
cp -r website/dist/* .
```

4. **Commit and push:**

```bash
git add .
git commit -m "Deploy to GitHub Pages"
git push origin gh-pages
```

5. **Configure GitHub Pages:**
   - Go to **Settings** > **Pages**
   - Select **Deploy from a branch**
   - Choose the `gh-pages` branch and `/ (root)` folder

### Option 4: Using Astro's Official GitHub Pages Deployment

Astro provides official GitHub Pages integration. You can use their recommended approach:

1. **Create `.github/workflows/deploy.yml`:**

```yaml
name: Deploy to GitHub Pages

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout your repository using git
        uses: actions/checkout@v4
      
      - name: Install, build, and upload your site
        uses: withastro/action@v2
        with:
          path: ./website
          node-version: 20
          package-manager: pnpm@9.12.2

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

2. Follow the same repository settings configuration as in Option 1.

## Troubleshooting

### Assets Not Loading

If CSS, JavaScript, or other assets aren't loading:

1. Verify that the `base` configuration in `website/astro.config.mjs` matches your repository name
2. Check that you're using the correct URL structure
3. Clear your browser cache

### 404 Errors

If you get 404 errors:

1. Ensure GitHub Pages is enabled in your repository settings
2. Verify the correct branch and folder are selected
3. Wait a few minutes after pushing - deployments can take time
4. Check the Actions tab for any deployment errors

### Build Failures

If the build fails:

1. Check the GitHub Actions logs for error messages
2. Try building locally first: `cd website && pnpm run build`
3. Ensure all dependencies are correctly specified in `package.json`
4. Verify that the Node.js version in the workflow matches your local version

## Comparing Deployment Options

| Option | Automation | Ease of Setup | Best For |
|--------|-----------|---------------|----------|
| GitHub Actions | High | Medium | Regular updates, team collaboration |
| Manual Deploy | Low | Easy | Occasional updates, full control |
| Branch Deploy | Medium | Medium | Simple setups, legacy projects |
| Astro Action | High | Easy | Astro-specific optimizations |

## Additional Resources

- [Astro GitHub Pages Deployment Guide](https://docs.astro.build/en/guides/deploy/github/)
- [GitHub Pages Documentation](https://docs.github.com/en/pages)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

## Differences from Codeberg Pages

The current repository is configured for Codeberg Pages deployment. Key differences when switching to GitHub Pages:

1. **Workflow Location**: GitHub uses `.github/workflows/` instead of `.forgejo/workflows/`
2. **Actions Syntax**: GitHub Actions syntax differs slightly from Forgejo/Gitea actions
3. **Domain Configuration**: GitHub Pages uses different domain structures
4. **Permissions**: GitHub Actions requires specific permissions for Pages deployment

Remember to update `website/astro.config.mjs` and `website/public/CNAME` accordingly when switching between platforms.
