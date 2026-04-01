package com.yuluo.yuluoaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedisKryoChatMemory implements ChatMemory {

    private static final Kryo kryo = new Kryo();
    private static final String KEY_PREFIX = "chat:memory:";
    private static final int EXPIRE_DAYS = 7;

    static {
        kryo.setRegistrationRequired(false);
        kryo.setReferences(true);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> conversationMessages = get(conversationId, Integer.MAX_VALUE);
        conversationMessages.addAll(messages);
        save(conversationId, conversationMessages);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String key = KEY_PREFIX + conversationId;
        String dataStr = stringRedisTemplate.opsForValue().get(key);

        if (dataStr == null) {
            return new ArrayList<>();
        }

        byte[] bytes = java.util.Base64.getDecoder().decode(dataStr);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Input input = new Input(bais);
        List<Message> messages = (List<Message>) kryo.readObject(input, ArrayList.class);
        input.close();

        if (lastN <= 0 || messages.size() <= lastN) {
            return messages;
        }
        return messages.subList(messages.size() - lastN, messages.size());
    }

    @Override
    public void clear(String conversationId) {
        stringRedisTemplate.delete(KEY_PREFIX + conversationId);
    }

    // ==================== 私有方法 ====================
    private void save(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, messages);
        output.close();

        String base64Data = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        stringRedisTemplate.opsForValue().set(key, base64Data, EXPIRE_DAYS, TimeUnit.DAYS);
    }
}