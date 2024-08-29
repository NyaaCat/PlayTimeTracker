package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.nyaacore.configuration.ISerializable;

public interface ISerializableExt extends ISerializable {

    /**
     * Validate the data in the object. may modify un-serilizable fields.
     * @return true if the data is valid, false otherwise.
     */
    boolean validate();
}
