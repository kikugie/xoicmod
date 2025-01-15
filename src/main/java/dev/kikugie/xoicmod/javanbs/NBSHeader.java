package dev.kikugie.xoicmod.javanbs;

import java.io.FileInputStream;
import java.io.IOException;

// https://github.com/omninbs/javanbs
public class NBSHeader {
   private int version;
   private int defaultInstruments;
   private int length;
   private int layerCount;
   private String name;
   private String author;
   private String originalAuthor;
   private String description;
   private int tempo;
   private boolean autoSaving;
   private int autoSavingDuration;
   private int timeSignature;
   private int minutesSpent;
   private int leftClicks;
   private int rightClicks;
   private int noteblocksAdded;
   private int noteblocksRemoved;
   private String songOrigin;
   private boolean looping;
   private int maxLoops;
   private int loopStart;

   public static NBSHeader readHeader(FileInputStream fis) throws IOException {
      int version = 0;
      NBSHeader header = new NBSHeader("");

      header.length = NBSReader.readBytes(fis, 2);

      if (header.length == 0) { // version > 0
         header.version = NBSReader.readBytes(fis, 1);
         header.defaultInstruments = NBSReader.readBytes(fis, 1);
         if (header.version >= 3) {header.length = NBSReader.readBytes(fis, 2);}
      }

      header.layerCount = NBSReader.readBytes(fis, 2);
      header.name = NBSReader.readString(fis);
      header.author = NBSReader.readString(fis);
      header.originalAuthor = NBSReader.readString(fis);
      header.description = NBSReader.readString(fis);
      header.tempo = NBSReader.readBytes(fis, 2);
      header.autoSaving = NBSReader.readBytes(fis, 1) != 0;
      header.autoSavingDuration = NBSReader.readBytes(fis, 1);
      header.timeSignature = NBSReader.readBytes(fis, 1);
      header.minutesSpent = NBSReader.readBytes(fis, 4);
      header.leftClicks = NBSReader.readBytes(fis, 4);
      header.rightClicks = NBSReader.readBytes(fis, 4);
      header.noteblocksAdded = NBSReader.readBytes(fis, 4);
      header.noteblocksRemoved = NBSReader.readBytes(fis, 4);
      header.songOrigin = NBSReader.readString(fis);

      if (header.version >= 4) {
         header.looping = NBSReader.readBytes(fis, 1) != 0;
         header.maxLoops = NBSReader.readBytes(fis, 1);
         header.loopStart = NBSReader.readBytes(fis, 2);
      }

      return header;
   }

   // Constructor
   public NBSHeader(String name) {
      this.name = name;
      this.version = 5;
      this.defaultInstruments = 15;
      this.length = 0;
      this.layerCount = 1;
      this.author = "";
      this.originalAuthor = "";
      this.description = "";
      this.tempo = 20;
      this.autoSaving = true;
      this.autoSavingDuration = 0;
      this.timeSignature = 0;
      this.minutesSpent = 0;
      this.leftClicks = 0;
      this.rightClicks = 0;
      this.noteblocksAdded = 0;
      this.noteblocksRemoved = 0;
      this.songOrigin = "";
      this.looping = false;
      this.maxLoops = 0;
      this.loopStart = 0;
    }


    // Setters
    public void setVersion(int version) {
        this.version = version;
    }

    public void setDefaultInstruments(int defaultInstruments) {
        this.defaultInstruments = defaultInstruments;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setLayerCount(int layerCount) {
        this.layerCount = layerCount;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setOriginalAuthor(String originalAuthor) {
        this.originalAuthor = originalAuthor;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    public void setAutoSaving(boolean autoSaving) {
        this.autoSaving = autoSaving;
    }

    public void setAutoSavingDuration(int autoSavingDuration) {
        this.autoSavingDuration = autoSavingDuration;
    }

    public void setTimeSignature(int timeSignature) {
        this.timeSignature = timeSignature;
    }

    public void setMinutesSpent(int minutesSpent) {
        this.minutesSpent = minutesSpent;
    }

    public void setLeftClicks(int leftClicks) {
        this.leftClicks = leftClicks;
    }

    public void setRightClicks(int rightClicks) {
        this.rightClicks = rightClicks;
    }

    public void setNoteblocksAdded(int noteblocksAdded) {
        this.noteblocksAdded = noteblocksAdded;
    }

    public void setNoteblocksRemoved(int noteblocksRemoved) {
        this.noteblocksRemoved = noteblocksRemoved;
    }

    public void setSongOrigin(String songOrigin) {
        this.songOrigin = songOrigin;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void setMaxLoops(int maxLoops) {
        this.maxLoops = maxLoops;
    }

    public void setLoopStart(int loopStart) {
        this.loopStart = loopStart;
    }

    // Getters
    public int getVersion() {
        return version;
    }

    public int getDefaultInstruments() {
        return defaultInstruments;
    }

    public int getLength() {
        return length;
    }

    public int getLayerCount() {
        return layerCount;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getOriginalAuthor() {
        return originalAuthor;
    }

    public String getDescription() {
        return description;
    }

    public int getTempo() {
        return tempo;
    }

    public boolean getAutoSaving() {
        return autoSaving;
    }

    public int getAutoSavingDuration() {
        return autoSavingDuration;
    }

    public int getTimeSignature() {
        return timeSignature;
    }

    public int getMinutesSpent() {
        return minutesSpent;
    }

    public int getLeftClicks() {
        return leftClicks;
    }

    public int getRightClicks() {
        return rightClicks;
    }

    public int getNoteblocksAdded() {
        return noteblocksAdded;
    }

    public int getNoteblocksRemoved() {
        return noteblocksRemoved;
    }

    public String getSongOrigin() {
        return songOrigin;
    }

    public boolean getLooping() {
        return looping;
    }

    public int getMaxLoops() {
        return maxLoops;
    }

    public int getLoopStart() {
        return loopStart;
    }
}
