package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.ISerializable;

import java.util.List;

public interface ISerializableExt extends ISerializable {

    /**
     * Validate the data in the object. may modify un-serilizable fields.
     * @param outputError a list to store the error message.
     *                    will be read from end to start.
     * @return true if the data is valid, false otherwise.
     */
    boolean validate(List<String> outputError);
}
