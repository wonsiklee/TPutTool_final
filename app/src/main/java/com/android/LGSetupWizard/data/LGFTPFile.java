package com.android.LGSetupWizard.data;

import android.support.annotation.NonNull;

import org.apache.commons.net.ftp.FTPFile;

public class LGFTPFile extends FTPFile implements Comparable<LGFTPFile> {

    public LGFTPFile() {
        super();
    }

    public LGFTPFile(FTPFile ftpFile) {
        this.setType(ftpFile.getType());
        this.setHardLinkCount(ftpFile.getHardLinkCount());
        this.setSize(ftpFile.getSize());
        this.setRawListing(ftpFile.getRawListing());
        this.setUser(ftpFile.getUser());
        this.setGroup(ftpFile.getGroup());
        this.setName(ftpFile.getName());
        this.setLink(ftpFile.getLink());
        this.setTimestamp(ftpFile.getTimestamp());

        this.setPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION, ftpFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION));
        this.setPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION, ftpFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION));
        this.setPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION, ftpFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION));

        this.setPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION, ftpFile.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION));
        this.setPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION, ftpFile.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION));
        this.setPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION, ftpFile.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION));

        this.setPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION, ftpFile.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION));
        this.setPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION, ftpFile.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION));
        this.setPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION, ftpFile.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION));
    }
    /*
     * @param   the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     *
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this object.
     */
    @Override
    public int compareTo(@NonNull LGFTPFile file) {
        // this 가 directory 이면 더 큰거니까 1 반환, 반대면 더 작은 거니까 -1 반환
        if (this.isDirectory() && file.isFile()) {
            return 1;
        } else if (this.isFile() && file.isDirectory()) {
            return -1;
        } else {
            return this.getName().compareTo(file.getName());
        }
    }
}
