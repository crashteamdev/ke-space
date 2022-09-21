package dev.crashteam.repricer.repository.redis

import dev.crashteam.repricer.repository.redis.entity.UserTokenEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface KeUserTokenRepository : CrudRepository<UserTokenEntity, String>
