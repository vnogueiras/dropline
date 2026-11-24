# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.2.0.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

## Styling: Angular Material vs Tailwind

We use both libraries, each for a different job. Follow these rules so they don't conflict.

### Who does what

| Use                  | For                                                                                                            |
| -------------------- | -------------------------------------------------------------------------------------------------------------- |
| **Angular Material** | Interactive components: buttons, form fields, select, dialog, snackbar, paginator, chips, progress indicators  |
| **Tailwind**         | Layout and spacing (`grid`, `flex`, `gap-*`, `p-*`, responsive `md:` / `lg:`), typography tweaks, custom cards |

### Rules

1. **Never restyle Material internals with Tailwind classes.** If a Material component needs to look different, use Material design tokens (`--mat-sys-*`) or the component's token overrides, not utility classes on its inner elements.
2. **Map Material tokens into Tailwind** so colors come from one source and stay consistent:

   ```css
   @theme {
     --color-primary: var(--mat-sys-primary);
     --color-surface: var(--mat-sys-surface);
     --color-on-surface: var(--mat-sys-on-surface);
   }
   ```

   Then use them as normal Tailwind utilities:

   ```html
   <div class="bg-surface text-on-surface p-4 rounded-lg">...</div>
   ```

3. **Dark mode** is driven by `color-scheme`, not a `dark:` class:

   ```css
   html {
     color-scheme: light dark;
   }
   ```

   A toggle button sets `color-scheme` on `<html>` to `light` or `dark`. The current choice lives in a signal and is persisted in `localStorage`:

   ```ts
   const stored = localStorage.getItem('color-scheme') as 'light' | 'dark' | null;
   readonly scheme = signal<'light' | 'dark'>(stored ?? 'light');

   constructor() {
     effect(() => {
       const value = this.scheme();
       document.documentElement.style.colorScheme = value;
       localStorage.setItem('color-scheme', value);
     });
   }

   toggle() {
     this.scheme.update(s => (s === 'light' ? 'dark' : 'light'));
   }
   ```

   Because Material's `--mat-sys-*` tokens respond to `color-scheme`, and Tailwind colors are mapped to those tokens, both switch together.
