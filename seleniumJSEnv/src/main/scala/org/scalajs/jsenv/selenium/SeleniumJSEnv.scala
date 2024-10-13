package org.scalajs.jsenv.selenium


import org.scalajs.jsenv._

import java.net.URL
import java.nio.file.{Path, Paths}
import org.openqa.selenium.Capabilities
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.WebDriver
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Platform
import SeleniumJSEnv.DriverFactory

final class SeleniumJSEnv(driverFactory: DriverFactory, config: SeleniumJSEnv.Config) extends JSEnv {

  val name: String = s"SeleniumJSEnv ($config)"

  def start(input: Seq[Input], runConfig: RunConfig): JSRun =
    SeleniumRun.start(newDriver _, input, config, runConfig)

  def startWithCom(input: Seq[Input], runConfig: RunConfig, onMessage: String => Unit): JSComRun =
    SeleniumRun.startWithCom(newDriver _, input, config, runConfig, onMessage)

  private def newDriver() = {
    val driver: WebDriver =
      driverFactory()

    /* The first `asInstanceOf`s are a fail-fast for the second one, which
     * scalac partially erases, so that we're sure right now that the last
     * cast is correct, as opposed to crashing when we call a method of
     * JavascriptExecutor on the driver.
     *
     * We are "allowed" to cast since we explicitly request JavascriptEnabled in
     * the capabilities.
     */
    driver.asInstanceOf[JavascriptExecutor]

    driver.asInstanceOf[WebDriver with JavascriptExecutor]
  }

  def this(driverFactory: DriverFactory) = this(driverFactory, SeleniumJSEnv.Config())
}

object SeleniumJSEnv {
  type DriverFactory = () => WebDriver

  final class Config private (
      val keepAlive: Boolean,
      val materialization: Config.Materialization
  ) {
    import Config.Materialization

    private def this() = this(
        keepAlive = false,
        materialization = Config.Materialization.Temp)

    /** Materializes purely virtual files into a temp directory.
     *
     *  Materialization is necessary so that virtual files can be referred to by
     *  name. If you do not know/care how your files are referred to, this is a
     *  good default choice. It is also the default of [[SeleniumJSEnv.Config]].
     */
    def withMaterializeInTemp: Config =
      copy(materialization = Materialization.Temp)

    /** Materializes files in a static directory of a user configured server.
     *
     *  This can be used to bypass cross origin access policies.
     *
     *  @param contentDir Static content directory of the server. The files will
     *      be put here. Will get created if it doesn't exist.
     *  @param webRoot URL making `contentDir` accessible thorugh the server.
     *      This must have a trailing slash to be interpreted as a directory.
     *
     *  @example
     *
     *  The following will make the browser fetch files using the http:// schema
     *  instead of the file:// schema. The example assumes a local webserver is
     *  running and serving the ".tmp" directory at http://localhost:8080.
     *
     *  {{{
     *  jsSettings(
     *    jsEnv := new SeleniumJSEnv(
     *        new org.openqa.selenium.firefox.FirefoxOptions(),
     *        SeleniumJSEnv.Config()
     *          .withMaterializeInServer(".tmp", "http://localhost:8080/")
     *    )
     *  )
     *  }}}
     */
    def withMaterializeInServer(contentDir: String, webRoot: String): Config =
      withMaterializeInServer(Paths.get(contentDir), new URL(webRoot))

    /** Materializes files in a static directory of a user configured server.
     *
     *  Version of `withMaterializeInServer` with stronger typing.
     *
     *  @param contentDir Static content directory of the server. The files will
     *      be put here. Will get created if it doesn't exist.
     *  @param webRoot URL making `contentDir` accessible thorugh the server.
     *      This must have a trailing slash to be interpreted as a directory.
     */
    def withMaterializeInServer(contentDir: Path, webRoot: URL): Config =
      copy(materialization = Materialization.Server(contentDir, webRoot))

    def withMaterialization(materialization: Materialization): Config =
      copy(materialization = materialization)

    def withKeepAlive(keepAlive: Boolean): Config =
      copy(keepAlive = keepAlive)

    private def copy(keepAlive: Boolean = keepAlive,
        materialization: Config.Materialization = materialization) = {
      new Config(keepAlive, materialization)
    }
  }

  object Config {
    def apply(): Config = new Config()

    abstract class Materialization private ()
    object Materialization {
      final case object Temp extends Materialization
      final case class Server(contentDir: Path, webRoot: URL) extends Materialization {
        require(webRoot.getPath().endsWith("/"), "webRoot must end with a slash (/)")
      }
    }
  }
}
