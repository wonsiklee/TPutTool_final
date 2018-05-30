package com.android.LGSetupWizard.clients;

public interface ILGFTPCopyStreamListener {
    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize);
}
