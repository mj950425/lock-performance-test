package com.example.stockstudy.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.config.Config
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    val redisProperties: RedisProperties
) {
    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        config.codec = StringCodec.INSTANCE  // StringCodec를 사용합니다.
        config.useSingleServer().address = "redis://" + redisProperties.host + ":" + redisProperties.port
        return Redisson.create(config)
    }
}

