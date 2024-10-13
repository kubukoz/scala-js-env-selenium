# scalajs-env-selenium

[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.17.svg)](https://www.scala-js.org/)

## Usage
Simply add the following line to your `project/plugins.sbt` (note that this line must be placed before `addSbtPlugin("org.scala-js" % "sbt-scalajs" % <scalajs-version>)`; otherwise you may get errors such as `java.lang.NoSuchMethodError: com.google.common.base.Preconditions.checkState` when you run tests):
```scala
libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "2.0.0"
```
and the following line to your sbt settings:
```scala
// Apply to the 'run' command
jsEnv := new org.scalajs.jsenv.selenium.SeleniumJSEnv(driverFactory)

// Apply to tests
(Test / jsEnv) := new org.scalajs.jsenv.selenium.SeleniumJSEnv(driverFactory)
```
where `driverFactory` is an implementation of the `org.scalajs.jsenv.selenium.DriverFactory` function type:

```scala
type DriverFactory = () => WebDriver
```

For example for Firefox:

```scala
jsEnv := new org.scalajs.jsenv.selenium.SeleniumJSEnv(
    () => new org.openqa.selenium.firefox.FirefoxDriver())
```

Selenium will download the appropriate binaries for you based on the driver your factory creates.

When executing the program with `run` a new browser window will be created,
the code will be executed in it and finally the browser will close itself.
All the console outputs will appear in SBT as usual. Executing `test` will open
several browser windows and close them all before the end of the tests.

### In browser debugging
If you wish to keep the browser window opened after the execution has terminated simply
add the option `withKeepAlive` on the environment:

``` scala
new SeleniumJSEnv(driverFactory, SeleniumJSEnv.Config().withKeepAlive(true))
```

It is recommend to use this with a `run` and not `test` because the latter tends
to leave too many browser windows open.

#### Debugging tests on a single window
By default tests are executed in their own window for parallelism.
When debugging tests with `withKeepAlive` it is possible to disable this option
using the `sbt` setting `(Test / parallelExecution) := false`.

### Headless Usage
It is often desirable to run Selenium headlessly.
This could be to run tests on a server without graphics, or to just prevent browser
windows popping up when running locally.

#### Driver options
**Note:** the availability and behavior of this configuration depends on the driver you choose. See the [noteworthy post from Selenium developers themselves][selenium-headless-going-away].

When creating a driver in your `DriverFactory`, you can provide additional options to specify behavior
such as headless mode.

For example, when using Firefox:

```scala
new FirefoxDriver(
  new FirefoxOptions()
    .addArguments("--headless")
)
```

For Chrome:

```scala
new ChromeDriver(
  new ChromeOptions()
    .addArguments("--headless")
)
```

#### xvfb
A common approach on Linux and Mac OSX, is to use `xvfb`, "X Virtual FrameBuffer".
It starts an X server headlessly, without the need for a graphics driver.

Once you have `xvfb` installed, usage here with SBT is as simple as:
```sh
Xvfb :1 &
DISPLAY=:1 sbt
```

The `:1` indicates the X display-number, which is a means to uniquely identify an
X server on a host. The `1` is completely arbitrary—you can choose any number so
long as there isn't another X server running that's already associated with it.

## License

`scalajs-env-selenium` is distributed under the
[BSD 3-Clause license](./LICENSE).

## Contributing

Follow the [contributing guide](./CONTRIBUTING.md).

[selenium-headless-going-away]: https://www.selenium.dev/blog/2023/headless-is-going-away/
