package org.jetbrains.noria;

import java.util.*;

public class LCS {

    public static int[] lcs(int[] a, int b[]) {
        int[][] lengths = new int[a.length+1][b.length+1];

        // row 0 and column 0 are initialized to 0 already
        int len = 0;
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < b.length; j++)
                if (a[i] == b[j])
                    lengths[i+1][j+1] = len = lengths[i][j] + 1;
                else
                    lengths[i+1][j+1] = len = Math.max(lengths[i+1][j], lengths[i][j+1]);

        // read the substring out from the matrix
        int[] result = new int[len];
        for (int x = a.length, y = b.length, i = 0;
             x != 0 && y != 0;) {
            if (lengths[x][y] == lengths[x-1][y])
                x--;
            else if (lengths[x][y] == lengths[x][y-1])
                y--;
            else {
                result[i++] = a[x-1];
                x--;
                y--;
            }
        }
        return result;
    }
}

