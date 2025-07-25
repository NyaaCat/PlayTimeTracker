package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.ISerializable;

public interface ISerializableExt extends ISerializable {



    /**
     * Validate the data in the object. may modify un-serilizable fields.
     * This method should be called after deserialization to ensure the object is in a valid state.
     * throws Exception if validation fails.
     * @param context validation context, can be used to access other configurations or services.
     */
    void validate(IValidationContext context) throws Exception;
}
