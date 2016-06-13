# think.config

The configuration library `think.config` is used to create an abstraction
around configuration files. It works by reading config files named
`*-config.edn` from the resources director in the uberjar (or in the local
repository if running on a repl). This defines a number of config variables and
values. An example is as follows:

### Config File:

    {
       :my-setting "value"
    }

Each of the settings can be a string, a number or a keyword.

### Usage:

    (require '[think.config :refer (get-config)])

    (get-config :my-setting)

The advantage to using the config library is that it provies several facilities beyond a standard config reader. 
* All of the settings are placed into the applications resource directory, which means that it's obvious which knobs a user can turn to configure the application. 
* The config settings are all displayed when the application starts up. This means that it is clear to see how the application was configured. 
* There is a merging operation where all of the `*-config.edn` files are read from the uberjar in reverse-alphabetical order. Which means that libraries can specify config settings and then the application can overwrite those settings to custom tune them with an `app-config.edn` file. 
* A config setting can be overwritten with an environment variable (e.g. `MY_SETTING=123 lein run`) to customize the application at runtime. It is also possible to place something like `_user-config.edn` in the resources directory (and git-ignore it) in order to provide development specific settings that don't need to be set with the environment every time.
* There is an option to parse the value as clojure data, which means that more advanced settings (e.g. lists and maps) can also be provided as config options.
