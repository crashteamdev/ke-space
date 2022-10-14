package dev.crashteam.repricer.proxy.interceptor

import dev.crashteam.repricer.client.ke.KazanExpressLkClient
import dev.crashteam.repricer.proxy.ProxyManager
import dev.crashteam.repricer.proxy.model.ProxyAddress
import dev.crashteam.repricer.repository.redis.CookieRepository
import dev.crashteam.repricer.repository.redis.entity.CookieEntity
import mu.KotlinLogging
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Predicate

private val log = KotlinLogging.logger {}

@Component
class CookieHeaderRequestInterceptor(
    private val cookieRepository: CookieRepository,
    private val proxyManager: ProxyManager,
) : ClientHttpRequestInterceptor {

    private val webDriverThreadLocal: ThreadLocal<ChromeBrowser> = ThreadLocal.withInitial {
        val randomProxy = proxyManager.getRandomProxy()
        ChromeBrowser(
            chromeDriver = newChromeDriver(randomProxy),
            proxyAddress = randomProxy,
        )
    }

    init {
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver")
        //WebDriverManager.chromedriver().setup()
    }

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val userId = request.headers[KazanExpressLkClient.USER_ID_HEADER]?.first()
        if (userId.isNullOrEmpty()) {
            return execution.execute(request, body)
        }
        val cookieEntity = cookieRepository.findById(userId).orElse(null)
        if (cookieEntity == null || cookieEntity.expiryAt.isBefore(LocalDateTime.now(ZoneId.of("UTC")))) {
            if (webDriverThreadLocal.get() == null) {
                val randomProxy = proxyManager.getRandomProxy()
                val chromeBrowser = ChromeBrowser(
                    chromeDriver = newChromeDriver(randomProxy),
                    proxyAddress = randomProxy,
                )
                webDriverThreadLocal.set(chromeBrowser)
            }
            val chromeBrowser = webDriverThreadLocal.get()
            val chromeDriver = chromeBrowser.chromeDriver
            try {
                val webDriverWait = WebDriverWait(chromeDriver, Duration.of(3, ChronoUnit.MINUTES))
                chromeDriver.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

                // Open yandex page
                chromeDriver.get("https://ya.ru")
                webDriverWait.until { ExpectedConditions.presenceOfElementLocated(By.className("search3__input")) }
                Thread.sleep(Random().nextLong(500, 1500))
                val searchInput = chromeDriver.findElement(By.className("search3__input")) // Find yandex input
                searchInput.click()
                searchInput.sendKeys("kazanexpress business") // Type KE site
                searchInput.sendKeys(Keys.RETURN)

                // Click KE site
                webDriverWait.until { ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href=\"https://business.kazanexpress.ru/\"]")) }
                Thread.sleep(Random().nextLong(500, 1500))
                chromeDriver.findElement(By.cssSelector("a[href=\"https://business.kazanexpress.ru/\"]"))
                    .click()

                // Switch to new tab
                Thread.sleep(Random().nextLong(500, 1500))
                val newTabName = (chromeDriver.windowHandles as HashSet).toArray()[1].toString()
                chromeDriver.switchTo().window(newTabName)
                webDriverWait.until { ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href=\"/seller/signin\"]")) }
                Thread.sleep(Random().nextLong(1000, 2000))
                chromeDriver.findElement(By.cssSelector("a[href=\"/seller/signin\"]")).click() // Click on signin button
                webDriverWait.until { ExpectedConditions.presenceOfElementLocated(By.cssSelector("button.solid")) }

                val qratorJsIdCookie = chromeDriver.manage().getCookieNamed("qrator_jsid")
                cookieRepository.save(
                    CookieEntity(
                        userId = userId,
                        proxyAddress = chromeBrowser.proxyAddress,
                        name = qratorJsIdCookie.name,
                        value = qratorJsIdCookie.value,
                        expiryAt = LocalDateTime.ofInstant(qratorJsIdCookie.expiry.toInstant(), ZoneId.of("UTC"))
                    )
                )
                request.headers.add("Cookie", "${qratorJsIdCookie.name}=${qratorJsIdCookie.value}")
            } catch (e: Exception) {
                log.error(e) { "Failed to get secure cookie. Page source = ${chromeDriver.pageSource}" }
                throw e
            } finally {
                chromeDriver.quit()
                webDriverThreadLocal.remove()
            }
        } else {
            request.headers.add("Cookie", "${cookieEntity.name}=${cookieEntity.value}")
        }

        return execution.execute(request, body)
    }

    private fun newChromeDriver(proxyAddress: ProxyAddress): ChromeDriver {
        val options = ChromeOptions()
        options.setHeadless(true)
        // Fixing 255 Error crashes
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")

        // Options to trick bot detection
        // Removing webdriver property
        options.addArguments("start-maximized")
        options.addArguments("--disable-extensions")
        options.addArguments("--disable-blink-features=AutomationControlled")
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"))
        options.setExperimentalOption("useAutomationExtension", false)
        options.addArguments("window-size=1920,1080")

        // Changing the user agent / browser fingerprint
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36")

        // Other
        options.addArguments("disable-infobars")

        val proxy = Proxy().apply {
            isAutodetect = false
            proxyType = Proxy.ProxyType.MANUAL
            httpProxy = "${proxyAddress.host}:${proxyAddress.port}"
            sslProxy = "${proxyAddress.host}:${proxyAddress.port}"
        }
        val desiredCapabilities = DesiredCapabilities()
        desiredCapabilities.setCapability(CapabilityType.PROXY, proxy)

        val chromeDriver = ChromeDriver(options.merge(desiredCapabilities))
        val uriPredicate = Predicate { uri: URI ->
            uri.host.contains(proxyAddress.host)
        }
        (chromeDriver as HasAuthentication).register(
            uriPredicate,
            UsernameAndPassword.of(proxyAddress.login, proxyAddress.password)
        )

        return chromeDriver
    }

    data class ChromeBrowser(
        val chromeDriver: ChromeDriver,
        val proxyAddress: ProxyAddress,
    )
}
