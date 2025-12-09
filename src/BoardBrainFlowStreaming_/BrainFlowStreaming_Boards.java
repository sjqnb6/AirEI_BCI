package BoardBrainFlowStreaming_;

import brainflow.BoardIds;

public enum BrainFlowStreaming_Boards
{
    CYTON("Cyton", BoardIds.CYTON_BOARD),
    CYTONDAISY("CytonDaisy", BoardIds.CYTON_DAISY_BOARD),
    GANGLION("Ganglion", BoardIds.GANGLION_BOARD),
    SYNTHETIC("Synthetic", BoardIds.SYNTHETIC_BOARD);

    private String name;
    private BoardIds boardId;

    BrainFlowStreaming_Boards(String _name, BoardIds _boardId) {
        this.name = _name;
        this.boardId = _boardId;
    }

    public String getName() {
        return name;
    }

    public BoardIds getBoardId() {
        return boardId;
    }
}
