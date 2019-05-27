import { Category, CategoryLogger, CategoryServiceFactory, CategoryConfiguration, LogLevel } from "typescript-logging";

// Optionally change default settings, in this example set default logging to Info.
// Without changing configuration, categories will log to Error.
CategoryServiceFactory.setDefaultConfiguration(new CategoryConfiguration(LogLevel.Info));

// Create categories, they will autoregister themselves, one category without parent (root) and a child category.
export const catServer = new Category("server");
export const catMain = new Category("main");

// Optionally get a logger for a category, since 0.5.0 this is not necessary anymore, you can use the category itself to log.
  // export const log: CategoryLogger = CategoryServiceFactory.getLogger(cat);