package dev.kikugie.xoicmod.javanbs;

// https://github.com/omninbs/javanbs
public class NBSNote {
   private int volume;
   private int pitch;
   private int instrument;
   private int key;
   private int stereo;
   private int layer;
   private int tick;

   public NBSNote(int instrument, int key, int pitch, int volume, int stereo) {
      this.instrument = instrument;
      this.key = key;
      this.pitch = pitch;
      this.volume = volume;
      this.stereo = stereo;

      this.layer = -1;
      this.tick = -1;
   } 

   // setters
   public void setVolume(int newVolume) {
      if (0 > newVolume || newVolume > 100) {throw new IllegalArgumentException("volume needs to be number between 0 and 100");}
      this.volume = newVolume;
   }

   public void setPitch(int newPitch) {
      if (-32768 > newPitch || newPitch > 32768) {throw new IllegalArgumentException("pitch needs to be a number between -32,768 and 32,768");}
      this.pitch = newPitch;
   }

   public void setInstrument(int newInstrument) {
      if (0 > newInstrument || newInstrument > 256) {throw new IllegalArgumentException("instrument needs to be number between 0 and 256");}
      this.instrument = newInstrument;
   }

   public void setKey(int newKey) {
      if (0 > newKey || newKey > 83) {throw new IllegalArgumentException("key needs to be a number between 0 and 83");}
      this.key = newKey;
   }

   public void setLayer(int newLayer) {
      if (-1 > newLayer) {throw new IllegalArgumentException("key needs to be a number above -1");}
      this.layer = newLayer;
   }

   public void setTick(int newTick) {
      if (-1 > newTick) {throw new IllegalArgumentException("tick needs to be a number above -1");}
      this.tick = newTick;
   }
   
   public void setStereo(int newStereo) {
      if (-100 > newStereo || newStereo >  100) {throw new IllegalArgumentException("stereo needs to be a number between -100 and 100");}
      this.stereo = newStereo;
   }


   // getters
   public int getVolume() {return volume;}

   public int getPitch() {return pitch;}

   public int getInstrument() {return instrument;}

   public int getKey() {return key;}

   public int getLayer() {return layer;}

   public int getTick() {return tick;}

   public int getStereo() {return stereo;}

}
