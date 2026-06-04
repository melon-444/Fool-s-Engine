// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.melon.foolsEngine.util;

import java.util.Arrays;

public class Signature {

    private final long[] bits;

    public Signature(int componentCapacity){

        int length = (componentCapacity + 63) / 64;

        bits = new long[length];
    }

    public void set(int componentID){
        int index = componentID >> 6;
        int bit = componentID & 63;

        bits[index] |= (1L << bit);
    }

    public void clear(int componentID){

        int index = componentID >> 6;
        int bit = componentID & 63;

        bits[index] &= ~(1L << bit);
    }

    public boolean matches(Signature system){
        for(int i=0;i<bits.length;i++){
            if((bits[i] & system.bits[i]) != system.bits[i])
                return false;
        }
        return true;
    }

    public boolean includes(Signature target){
        lenCheck(target);
        for(int i=0;i<bits.length;i++){
            if((bits[i] & target.bits[i]) == target.bits[i])
                continue;
            else
                //System.out.println("debug false");
                return false;

        }
        //System.out.println("debug true");
        return true;
    }

    public Signature mix(Signature target
    ){
        lenCheck(target);
        for(int i=0;i<bits.length;i++){
            this.bits[i] |= target.bits[i];
        }
        return this;
    }

    private void lenCheck(Signature target){
        if(target.bits.length != bits.length) throw new RuntimeException("Signature length didn't match");
    }

    public String toString(){
        StringBuilder sb = new StringBuilder("Signature{ bits=");
        for(long bit :bits)
        {   sb.append("[");
            for(int i=64;i>=0;i--)
                sb.append(((bit & (1L << i))== 0L)?"0":"1");
            sb.append("]=");
            sb.append(bit);
        }
        sb.append("}");
        return sb.toString();
    }

}