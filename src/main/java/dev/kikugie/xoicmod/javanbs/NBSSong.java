package dev.kikugie.xoicmod.javanbs;

import java.util.ArrayList;
import java.util.List;

// https://github.com/omninbs/javanbs
public class NBSSong {
   private NBSHeader header;
   private List<NBSNote> notes;

   public NBSSong(String name) {
      notes = new ArrayList<>();
      header = new NBSHeader(name);
   }

   public NBSHeader getHeader() {return header;}
   
   public List<NBSNote> getNotes() {return notes;}


   public void setHeader(NBSHeader header) {this.header = header;}
}
