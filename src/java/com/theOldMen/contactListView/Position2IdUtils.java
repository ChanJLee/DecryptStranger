package com.theOldMen.contactListView;


import java.util.HashMap;

/**
 * Created by cheng on 2015/4/22.
 */
public class Position2IdUtils {

    public static final String groupPosition = "GROUPPOSITION";
    public static final String childPosition = "CHILDPOSITION";

    public static long position2Id(int groupId, int childId) {
        long id = groupId + (childId + 1) * (1L << 31);
        return id;
    }

    public static HashMap<String, Integer> Id2Position(long id){
        long childId = (id / (1L << 31)) - 1;
        long groupId = id - (childId + 1) * (1L << 31);
        Integer group = new Integer((int)groupId);
        Integer child = new Integer((int)childId);
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put(groupPosition, group);
        hashMap.put(childPosition, child);
        return hashMap;
    }
}
