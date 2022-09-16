package dev.crashteam.repricer.config

import dev.crashteam.repricer.client.ke.KazanExpressLkClient
import dev.crashteam.repricer.config.properties.RepricerProperties
import dev.crashteam.repricer.repository.redis.UserCookieRepository
import dev.crashteam.repricer.repository.redis.entity.CookieEntity
import mu.KotlinLogging
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class CookieHeaderRequestInterceptor(
    private val userCookieRepository: UserCookieRepository,
    private val repricerProperties: RepricerProperties,
) : ClientHttpRequestInterceptor {

    private val webDriverThreadLocal: ThreadLocal<ChromeDriver> = ThreadLocal.withInitial { newChromeDriver() }

    init {
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver")
    }

    // TODO: add dynamic proxy
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        if (!repricerProperties.cookieBotProtectionBypassEnabled) {
            return execution.execute(request, body)
        }
        val userId = request.headers[KazanExpressLkClient.USER_ID_HEADER]!!.first()
        val cookieEntity = userCookieRepository.getCookie(userId)
        if (cookieEntity == null || cookieEntity.expiryAt.isBefore(LocalDateTime.now())) {
            if (webDriverThreadLocal.get() == null) {
                webDriverThreadLocal.set(newChromeDriver())
            }
            val webDriver = webDriverThreadLocal.get()
            try {
                val webDriverWait = WebDriverWait(webDriver, Duration.of(120, ChronoUnit.SECONDS))
                webDriver.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

                // Open yandex page
                webDriver.get("https://ya.ru")
                webDriverWait.until { ExpectedConditions.presenceOfElementLocated(By.className("search3__input")) }
                Thread.sleep(Random().nextLong(600, 1200))
                val searchInput = webDriver.findElement(By.className("search3__input")) // Find yandex input
                searchInput.click()
                searchInput.sendKeys("kazanexpress business") // Type KE site
                searchInput.sendKeys(Keys.RETURN)

                // Click KE site
                webDriverWait.until { ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href=\"https://business.kazanexpress.ru/\"]")) }
                Thread.sleep(Random().nextLong(600, 1200))
                webDriver.findElement(By.cssSelector("a[href=\"https://business.kazanexpress.ru/\"]"))
                    .click()

                // Switch to new tab
                Thread.sleep(Random().nextLong(600, 1200))
                val newTabName = (webDriver.windowHandles as HashSet).toArray()[1].toString()
                webDriver.switchTo().window(newTabName)
                webDriverWait.until { ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href=\"/seller/signin\"]")) }
                webDriver.findElement(By.cssSelector("a[href=\"/seller/signin\"]")).click() // Click on signin button
                webDriverWait.until { ExpectedConditions.presenceOfElementLocated(By.cssSelector("button.solid")) }

                val qratorJsIdCookie = webDriver.manage().getCookieNamed("qrator_jsid")
                userCookieRepository.saveCookie(
                    userId,
                    CookieEntity(
                        name = qratorJsIdCookie.name,
                        value = qratorJsIdCookie.value,
                        expiryAt = LocalDateTime.ofInstant(qratorJsIdCookie.expiry.toInstant(), ZoneId.of("UTC"))
                    )
                )
                request.headers.add("Cookie", "${qratorJsIdCookie.name}=${qratorJsIdCookie.value}")
            } catch (e: Exception) {
                log.error(e) { "Failed to get secure cookie. Page source = ${webDriver.pageSource}" }
                throw e
            } finally {
                webDriver.quit()
                webDriverThreadLocal.remove()
            }
        } else {
            request.headers.add("Cookie", "${cookieEntity.name}=${cookieEntity.value}")
        }
        request.headers.remove(KazanExpressLkClient.USER_ID_HEADER)

        return execution.execute(request, body)
    }

    private fun newChromeDriver(): ChromeDriver {
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
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.5195.52 Safari/537.36")

        // Other
        options.addArguments("disable-infobars")

        return ChromeDriver(options)
    }
}
