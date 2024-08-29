package cat.nyaa.playtimetracker.reward;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.function.BiFunction;

public class TestRewardsSerialize {

    @Test
    public void testEcoRewardData() throws NoSuchFieldException, IllegalAccessException {
        Class<EcoReward> klass = EcoReward.class;
        Field fieldAmount = klass.getDeclaredField("amount");
        fieldAmount.setAccessible(true);
        Field fieldRollbackFlag = klass.getDeclaredField("rollbackFlag");
        fieldRollbackFlag.setAccessible(true);
        Field fieldRollbackPlayerUUID = klass.getDeclaredField("rollbackPlayerUUID");
        fieldRollbackPlayerUUID.setAccessible(true);


        final UUID playerUUID = UUID.randomUUID();
        {
            EcoReward ecoReward = new EcoReward();
            fieldAmount.set(ecoReward, 1.0);
            fieldRollbackFlag.set(ecoReward, 2);
            fieldRollbackPlayerUUID.set(ecoReward, playerUUID);

            byte[] bytes = null;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ecoReward.serialize(baos);
                bytes = baos.toByteArray();
            } catch (Exception e) {
                Assertions.fail(e);
            }

            ecoReward = new EcoReward();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                ecoReward.deserialize(bais);
            } catch (Exception e) {
                Assertions.fail(e);
            }

            Assertions.assertEquals(1.0, fieldAmount.get(ecoReward));
            Assertions.assertEquals(2, fieldRollbackFlag.get(ecoReward));
            Assertions.assertEquals(playerUUID, fieldRollbackPlayerUUID.get(ecoReward));
        }

        {
            EcoReward ecoReward = new EcoReward();
            fieldAmount.set(ecoReward, 5.2);
            fieldRollbackFlag.set(ecoReward, 1);

            byte[] bytes = null;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ecoReward.serialize(baos);
                bytes = baos.toByteArray();
            } catch (Exception e) {
                Assertions.fail(e);
            }

            ecoReward = new EcoReward();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                ecoReward.deserialize(bais);
            } catch (Exception e) {
                Assertions.fail(e);
            }

            Assertions.assertEquals(5.2, fieldAmount.get(ecoReward));
            Assertions.assertEquals(1, fieldRollbackFlag.get(ecoReward));
            Assertions.assertNull(fieldRollbackPlayerUUID.get(ecoReward));
        }
    }

}
