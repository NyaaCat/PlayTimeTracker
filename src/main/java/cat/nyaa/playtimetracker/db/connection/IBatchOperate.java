package cat.nyaa.playtimetracker.db.connection;

public interface IBatchOperate {

    void beginBatchMode();


    void endBatchMode();
}
