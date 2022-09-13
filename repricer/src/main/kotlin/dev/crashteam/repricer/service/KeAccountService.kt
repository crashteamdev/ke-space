package dev.crashteam.repricer.service

import dev.crashteam.repricer.db.model.enums.InitializeState
import dev.crashteam.repricer.db.model.enums.MonitorState
import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.job.KeAccountInitializeJob
import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountEntity
import dev.crashteam.repricer.restriction.AccountSubscriptionRestrictionValidator
import dev.crashteam.repricer.service.encryption.PasswordEncryptor
import dev.crashteam.repricer.service.error.AccountItemPoolLimitExceededException
import dev.crashteam.repricer.service.error.UserNotFoundException
import mu.KotlinLogging
import org.quartz.JobBuilder
import org.quartz.ObjectAlreadyExistsException
import org.quartz.Scheduler
import org.quartz.SimpleTrigger
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class KeAccountService(
    private val accountRepository: AccountRepository,
    private val keAccountRepository: KeAccountRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val accountRestrictionValidator: AccountSubscriptionRestrictionValidator,
    private val scheduler: Scheduler,
) {

    fun addKeAccount(userId: String, login: String, password: String): KazanExpressAccountEntity {
        log.debug { "Add ke account. userId=$userId; login=$login; password=*****" }
        val accountEntity = accountRepository.getAccount(userId)
            ?: throw UserNotFoundException("Not found user by id=${userId}")
        val isValidKeAccountCount = accountRestrictionValidator.validateKeAccountCount(userId)

        if (!isValidKeAccountCount)
            throw AccountItemPoolLimitExceededException("Account limit exceeded for user. userId=$userId")

        val encryptedPassword = passwordEncryptor.encryptPassword(password)
        val kazanExpressAccountEntity = KazanExpressAccountEntity(
            id = UUID.randomUUID(),
            accountId = accountEntity.id!!,
            login = login,
            password = Base64.getEncoder().encodeToString(encryptedPassword),
        )
        keAccountRepository.save(kazanExpressAccountEntity)

        return kazanExpressAccountEntity
    }

    fun removeKeAccount(userId: String, keAccountId: UUID): Int {
        log.debug { "Remove ke account. userId=$userId; keAccountId=$keAccountId" }
        return keAccountRepository.removeKazanExpressAccount(userId, keAccountId)
    }

    fun getKeAccounts(userId: String): List<KazanExpressAccountEntity> {
        return keAccountRepository.getKazanExpressAccounts(userId)
    }

    fun getKeAccount(userId: String, keAccountId: UUID): KazanExpressAccountEntity? {
        return keAccountRepository.getKazanExpressAccount(userId, keAccountId)
    }

    fun editKeAccount(userId: String, keAccountId: UUID, login: String, password: String): KazanExpressAccountEntity {
        val kazanExpressAccount = keAccountRepository.getKazanExpressAccount(userId, keAccountId)
            ?: throw UserNotFoundException("Not found user by id=${userId}")
        if (kazanExpressAccount.initializeState == InitializeState.in_progress) {
            throw IllegalStateException("Not allowed to change account credential while initialization in progress")
        }
        if (kazanExpressAccount.updateState == UpdateState.in_progress) {
            throw IllegalStateException("Not allowed to change account credential while update state in progress")
        }
        val encryptedPassword = passwordEncryptor.encryptPassword(password)
        val updatedKeAccount =
            kazanExpressAccount.copy(login = login, password = Base64.getEncoder().encodeToString(encryptedPassword))
        keAccountRepository.save(updatedKeAccount)

        return updatedKeAccount
    }

    @Transactional
    fun initializeKeAccountJob(userId: String, keAccountId: UUID): Boolean {
        keAccountRepository.getKazanExpressAccount(userId, keAccountId)
            ?: throw IllegalArgumentException("Not found KE account. userId=$userId;keAccountId=$keAccountId")
        val jobIdentity = "$keAccountId-keaccount-initialize-job"
        val jobDetail =
            JobBuilder.newJob(KeAccountInitializeJob::class.java).withIdentity(jobIdentity).build()
        val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
            setName(jobIdentity)
            setStartTime(Date())
            setRepeatInterval(0L)
            setRepeatCount(0)
            setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
            setPriority(Int.MAX_VALUE)
            afterPropertiesSet()
        }.getObject()
        jobDetail.jobDataMap["userId"] = userId
        jobDetail.jobDataMap["keAccountId"] = keAccountId
        try {
            scheduler.scheduleJob(jobDetail, triggerFactoryBean)
            keAccountRepository.changeInitializeState(
                userId,
                keAccountId,
                InitializeState.in_progress
            )

            return true
        } catch (e: ObjectAlreadyExistsException) {
            log.warn { "Task still in progress: $jobIdentity" }
        } catch (e: Exception) {
            log.error(e) { "Failed to start scheduler job" }
        }
        return false
    }

    fun changeKeAccountMonitoringState(userId: String, keAccountId: UUID, monitorState: MonitorState): Int {
        log.debug { "Change ke account monitor state. userId=$userId; keAccountId=$keAccountId; monitorState=$monitorState" }
        return keAccountRepository.changeMonitorState(userId, keAccountId, monitorState)
    }

}
