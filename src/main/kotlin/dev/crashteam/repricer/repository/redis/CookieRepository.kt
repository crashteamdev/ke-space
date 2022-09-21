package dev.crashteam.repricer.repository.redis

import dev.crashteam.repricer.repository.redis.entity.CookieEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CookieRepository : CrudRepository<CookieEntity, String>
