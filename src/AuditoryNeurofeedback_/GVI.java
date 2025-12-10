package AuditoryNeurofeedback_;

import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import ddf.minim.ugens.FilePlayer;

public class GVI {
    public static Minim minim;
    public FilePlayer[] auditoryNfbFilePlayers;
    public static ddf.minim.ugens.Gain[] auditoryNfbGains;
    public static AudioOutput audioOutput;
    public static boolean audioOutputIsAvailable;
}
