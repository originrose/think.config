# think.config [![Build Status](https://travis-ci.com/thinktopic/think.config.svg?token=64MLcsqSTjE7SCpD6LB1&branch=master)](https://travis-ci.com/thinktopic/think.config)

Add this to your project.clj:
```
[thinktopic/think.config "0.2.3"]
```

`think.config` is a library that creates  an abstraction around configuration
files. It works by reading config files named `*-config.edn` from the resources
directory in the uberjar (or in the local repository if running on a repl).
This defines a number of config variables and values. An example is as follows:

### Config File (e.g. `app-config.edn`):

    {
       :my-setting "value"
    }

Each of the settings can be any type, the final type of the value will be
decided by base of the precedence heirarchy defined below.

### Usage:

#### Getting config values

    (require '[think.config :refer [get-config]])

    (get-config :my-setting)

#### Overwriting config values

In the event that you wish to programatically overwrite a config setting, it is
possible to use the `with-config` macro as follows:

```
(require '[think.config :refer [get-config with-config]])

(with-config [:my-setting true]
 (get-config :my-setting))       ; => true
```


### Precedence Heirarchy

`think.config` allows the user of the library to specify config values in
several different ways. Any `*-config.edn` found within the application or a
dependency will be merged into the config. The order that this occurs is
reverse alphabetical with `app-config.edn` and `user-config.edn` moved to the
top of the stack respectively.

Next, values set either through the `:env` key within a leiningen profile or
through the environment will take precedence over those specified in
`*-config.edn` files.

In summary, this hierarchy results in the following ordering (furthest to the left takes precedence):
`with-config` ➡ `environment` ➡ `:env` in profile.clj ➡  `user-config.edn` ➡ `app-config.edn` ➡ `libraries (a-z)`

### Types

One major advantage over other configuration options that `think.config` provides is types. The bottom of the configuration stack defines the type (i.e. when a library specifies a default value, it also specifies the type because the `.edn` files are typed. Any configuration layer that overwrites this value gets coerced to the type specified at the base. As a consequence, things specified through the environment (or the command line) which come in a strings will be converted to the appropriate type and the application can read these types without performing the conversion on its own.

### Why another configuration library?

The advantage to using the config library is that it provies several facilities
beyond a standard config reader. 
* All of the settings are placed into the applications resource directory,
  which means that it's obvious which knobs a user can turn to configure the
  application. 
* The config settings are all available to the user through the
  `get-config-table-str` function. This means that it is clear to see how the
  application was configured. Note that string values will be quoted when returned
  from this call, making it obvious what type the final value is.
* There is a merging operation where all of the `*-config.edn` files are read
  from the uberjar in reverse-alphabetical order (with `user-config.edn` and
  `app-config.edn` handled specially as mentioned above).. Which means that
  libraries can specify config settings and then the application can overwrite
  those settings to custom tune them with an `app-config.edn` file.
* A config setting can be overwritten with an environment variable (e.g.
  `MY_SETTING=123 lein run`) to customize the application at runtime. It is
  also possible to place a file named `user-config.edn` in the resources
  directory (and git-ignore it) in order to provide development specific
  settings that don't need to be set with the environment every time.
* Types are defined by the base of the precedence heirarchy (see above) which
  means that they can be set using environment variables and read as actual
  types within the code without having to perform something like `read-string`
  on each of them.
