package net.jxta.impl.xindice.core.filer;

/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xindice" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999-2001, The dbXML
 * Group, L.L.C., http://www.dbxmlgroup.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * $Id: Paged.java,v 1.1 2007/01/16 11:01:32 thomas Exp $
 */

import net.jxta.impl.xindice.core.DBException;
import net.jxta.impl.xindice.core.FaultCodes;
import net.jxta.impl.xindice.core.data.Key;
import net.jxta.impl.xindice.core.data.Value;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

/**
 * Paged is a paged file foundation that is used by both the BTree
 * class and the HashFiler.  It provides flexible paged I/O and
 * page caching functionality.
 */

public abstract class Paged {
    /**
     *  Log4J Logger
     **/
    private final static Logger LOG = Logger.getLogger(Paged.class.getName());    

    // The maximum number of pages that will be held in the dirty cache.
    private static final int MAX_DIRTY_SIZE = 128;

    // The maximum number of open random access files we can have
    private static final int MAX_DESCRIPTORS = 16;

    protected static final byte UNUSED = 0;
    protected static final byte OVERFLOW = 126;
    protected static final byte DELETED = 127;

    protected static final int NO_PAGE = -1;
    // flag whether to sync DB on every write or not.
    protected boolean sync = true;

    // Cache of recently read pages.
    private Map pages = new WeakHashMap();

    // Cache of modified pages waiting to be written out.
    private Map dirty = new HashMap();

    // Random access file cache.
    private Stack descriptors = new Stack();

    // The number of random access file objects that exist. Either in the cache
    // or in use.
    private int descCount = 0;

    // Whether the file is opened or not.
    private boolean opened = false;

    // The underlying file where the Paged object stores its pages.
    private File file;

    private FileHeader fileHeader;

    public Paged() {
        fileHeader = createFileHeader();
    }

    public Paged(File file) {
        this();
        setFile(file);
    }

    /**
     * setFile sets the file object for this Paged.
     *
     * @param file The File
     */
    protected final void setFile(final File file) {
        this.file = file;
    }

    /**
     * getFile returns the file object for this Paged.
     *
     * @return The File
     */
    protected final File getFile() {
        return file;
    }

    protected final RandomAccessFile getDescriptor() throws IOException {
        synchronized (descriptors) {
        // If there are descriptors in the cache return one.
        if (!descriptors.empty()) {
            return (RandomAccessFile) descriptors.pop();
        }
        // Otherwise we need to get one some other way.
        else {
            // First try to create a new one if there's room
            if (descCount < MAX_DESCRIPTORS) {
                descCount++;
                return new RandomAccessFile(file, "rw");
            }
            // Otherwise we have to wait for one to be released by another thread.
            else {
                while (true) {
                    try {
                            descriptors.wait();
                        return (RandomAccessFile) descriptors.pop();
                        } catch (InterruptedException e) {
                            // Ignore, and continue to wait
                        } catch (EmptyStackException e) {
                            // Ignore, and continue to wait
                        }
                    }
                }
            }
        }
    }

    /**
     * Puts a RandomAccessFile ('descriptor') back into the descriptor pool.
     */
    protected final void putDescriptor(RandomAccessFile raf) {
        if (raf != null) {
            synchronized (descriptors) {
            descriptors.push(raf);
                descriptors.notify();
            }
        }
    }


    /**
     * Closes a RandomAccessFile ('descriptor') and removes it from the pool.
     */
    protected final void closeDescriptor(RandomAccessFile raf) {
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                // Ignore close exception
            }

            // Synchronization is necessary as decrement operation is not atomic
            synchronized (descriptors) {
                descCount --;
            }
        }
    }

    /**
     * getPage returns the page specified by pageNum.
     *
     * @param lp The Page number
     * @return The requested Page
     * @throws IOException if an Exception occurs
     */
    protected final Page getPage(Long lp) throws IOException {
        Page p;
        synchronized (this) {
            // Check if it's in the dirty cache
            p = (Page) dirty.get(lp);

            // if not check if it's already loaded in the page cache
            if (p == null) {
                p = (Page) pages.get(lp);
            }

            // if still not found we need to create it and add it to the page cache.
            if (p == null) {
                p = new Page(lp.longValue());
                pages.put(lp, p);
            }
        }

        // Load the page from disk if necessary
        synchronized (p) {
            if (!p.isLoaded()) {
                p.read();
                p.setLoaded(true);
            }
        }

        return p;
    }

    /**
     * getPage returns the page specified by pageNum.
     *
     * @param pageNum The Page number
     * @return The requested Page
     * @throws IOException if an Exception occurs
     */
    protected final Page getPage(long pageNum) throws IOException {
        return getPage(new Long(pageNum));
    }

    /**
     * readValue reads the multi-Paged Value starting at the specified
     * Page.
     *
     * @param page The starting Page
     * @return The Value
     * @throws IOException if an Exception occurs
     */
    protected final Value readValue(Page page) throws IOException {
        PageHeader sph = page.getPageHeader();
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int)sph.getRecordLen());
        Page p = page;
        PageHeader ph = null;
        long nextPage;

        // Loop until we've read all the pages into memory.
        while (true) {
            ph = p.getPageHeader();

            // Add the contents of the page onto the stream
            p.streamTo(bos);

            // Continue following the list of pages until we get to the end.
            nextPage = ph.getNextPage();
            if (nextPage != NO_PAGE) {
                p = getPage(nextPage);
            } else {
                break;
            }
        }

        // Return a Value with the collected contents of all pages.
        return new Value(bos.toByteArray());
    }

    /**
     * readValue reads the multi-Paged Value starting at the specified
     * page number.
     *
     * @param page The starting page number
     * @return The Value
     * @throws IOException if an Exception occurs
     */
    protected final Value readValue(long page) throws IOException {
        return readValue(getPage(page));
    }

    /**
     * writeValue writes the multi-Paged Value starting at the specified
     * Page.
     *
     * @param page The starting Page
     * @param value The Value to write
     * @throws IOException if an Exception occurs
     */
    protected final void writeValue(Page page, Value value) throws IOException {
        if (value == null) {
            throw new IOException("Can't write a null value");
        }

        InputStream is = value.getInputStream();

        // Write as much as we can onto the primary page.
        PageHeader hdr = page.getPageHeader();
        hdr.setRecordLen(value.getLength());
        page.streamFrom(is);

        // Write out the rest of the value onto any needed overflow pages
        while (is.available() > 0) {
            Page lpage = page;
            PageHeader lhdr = hdr;

            // Find an overflow page to use
            long np = lhdr.getNextPage();
            if (np != NO_PAGE) {
                // Use an existing page.
                page = getPage(np);
            } else {
                // Create a new overflow page
                page = getFreePage();
                lhdr.setNextPage(page.getPageNum());
            }

            // Mark the page as an overflow page.
            hdr = page.getPageHeader();
            hdr.setStatus(OVERFLOW);

            // Write some more of the value to the overflow page.
            page.streamFrom(is);

            lpage.write();
        }

        // Cleanup any unused overflow pages. i.e. the value is smaller then the
        // last time it was written.
        long np = hdr.getNextPage();
        if (np != NO_PAGE) {
            unlinkPages(np);
        }

        hdr.setNextPage(NO_PAGE);
        page.write();
    }

    /**
     * writeValue writes the multi-Paged Value starting at the specified
     * page number.
     *
     * @param page The starting page number
     * @param value The Value to write
     * @throws IOException if an Exception occurs
     */
    protected final void writeValue(long page, Value value) throws IOException {
        writeValue(getPage(page), value);
    }

    /**
     * unlinkPages unlinks a set of pages starting at the specified Page.
     *
     * @param page The starting Page to unlink
     * @throws IOException if an Exception occurs
     */
    protected final void unlinkPages(Page page) throws IOException {
        // Handle the page if it's in primary space by setting its status to
        // DELETED and freeing any overflow pages linked to it.
        if (page.pageNum < fileHeader.pageCount) {
            long nextPage = page.header.nextPage;
            page.header.setStatus(DELETED);
            page.header.setNextPage(NO_PAGE);
            page.write();

            // See if there are any chained pages from the page that was just removed
            if (nextPage == NO_PAGE) {
                page = null;
            } else {
                page = getPage(nextPage);
            }
        }

        // Add any overflow pages to the list of free pages.
        if (page != null) {
            // Get the first page in the chain.
            long firstPage = page.pageNum;

            // Find the last page in the chain.
            while (page.header.nextPage != NO_PAGE) {
                page = getPage(page.header.nextPage);
            }
            long lastPage = page.pageNum;

            // If there are already some free pages, add the start of the chain
            // to the list of free pages.
            if (fileHeader.lastFreePage != NO_PAGE) {
                Page p = getPage(fileHeader.lastFreePage);
                p.header.setNextPage(firstPage);
                p.write();
            }

            // Otherwise set the chain as the list of free pages.
            if (fileHeader.firstFreePage == NO_PAGE) {
                fileHeader.setFirstFreePage(firstPage);
            }

            // Add a reference to the end of the chain.
            fileHeader.setLastFreePage(lastPage);
        }
    }

    /**
     * unlinkPages unlinks a set of pages starting at the specified
     * page number.
     *
     * @param pageNum The starting page number to unlink
     * @throws IOException if an Exception occurs
     */
    protected final void unlinkPages(long pageNum) throws IOException {
        unlinkPages(getPage(pageNum));
    }

    /**
     * getFreePage returns the first free Page from secondary storage.
     * If no Pages are available, the file is grown as appropriate.
     *
     * @return The next free Page
     * @throws IOException if an Exception occurs
     */
    protected final Page getFreePage() throws IOException {
        Page p = null;
        long pageNum = fileHeader.firstFreePage;
        if (pageNum != NO_PAGE) {
            // Steal a deleted page
            p = getPage(pageNum);
            fileHeader.setFirstFreePage(p.getPageHeader().nextPage);
            if (fileHeader.firstFreePage == NO_PAGE) {
                fileHeader.setLastFreePage(NO_PAGE);
	    }
        } else {
            // Grow the file
            pageNum = fileHeader.totalCount;
            fileHeader.setTotalCount(pageNum + 1);
            p = getPage(pageNum);
        }

        // Initialize The Page Header (Cleanly)
        p.header.setNextPage(NO_PAGE);
        p.header.setStatus(UNUSED);
        return p;
    }

    protected final void checkOpened() throws DBException {
        if (!opened) {
            throw new FilerException(FaultCodes.COL_COLLECTION_CLOSED, "Filer is closed");
	}
    }

    /**
     * getFileHeader returns the FileHeader
     *
     * @return The FileHeader
     */
    public FileHeader getFileHeader() {
        return fileHeader;
    }

    public boolean exists() {
        return file.exists();
    }

    public boolean create() throws DBException {
        RandomAccessFile raf = null;
        try {
            raf = getDescriptor();
            fileHeader.write();
            flush();
            raf.close();
            return true;
        } catch (Exception e) {
            throw new FilerException(FaultCodes.GEN_CRITICAL_ERROR, "Error creating " + file.getName(), e);
        } finally {
            //putDescriptor(raf);
            descCount -= 1;
        }
    }

    public boolean open() throws DBException {
        RandomAccessFile raf = null;
        try {
            if (exists()) {
                raf = getDescriptor();
                fileHeader.read();
                opened = true;
            } else {
                opened = false;
	    }
            return opened;
        } catch (Exception e) {
            throw new FilerException(FaultCodes.GEN_CRITICAL_ERROR, "Error opening " + file.getName(), e);
        } finally {
            putDescriptor(raf);
        }
    }

    public synchronized boolean close() throws DBException {
        try {
            if (isOpened()) {
                flush();
                opened = false;
                synchronized (descriptors) {
                    final int total = descCount;
                    // Close descriptors in cache
                while (!descriptors.empty()) {
                        closeDescriptor((RandomAccessFile)descriptors.pop());
                    }
                    // Attempt to close descriptors in use. Max wait time = 0.5s * MAX_DESCRIPTORS
                    int n = descCount;
                    while (descCount > 0 && n > 0) {
                        descriptors.wait(500);
                        if (descriptors.isEmpty()) {
                            n--;
                        } else {
                            closeDescriptor((RandomAccessFile)descriptors.pop());
                        }
                    }
                    if (descCount > 0) {
                        LOG.warn(descCount + " out of " + total + " files were not closed during close.");
                    }
                }
                return true;
            } else
                return false;
        } catch (Exception e) {
            throw new FilerException(FaultCodes.GEN_CRITICAL_ERROR, "Error closing " + file.getName(), e);
        }
    }

    public boolean isOpened() {
        return opened;
    }

    public boolean drop() throws DBException {
        try {
            close();
            if (exists()) {
                return getFile().delete();
	    } else {
                return true;
	    }
        } catch (Exception e) {
            throw new FilerException(FaultCodes.COL_CANNOT_DROP, "Can't drop " + file.getName(), e);
        }
    }

    public void flush() throws DBException {
        // TODO: Clean up this code
        boolean error = false;
        synchronized (this) {
            Iterator i = dirty.values().iterator();

            while (i.hasNext()) {
                Page p = (Page) i.next();
                try {
                    p.flush();
                } catch (Exception e) {
                    error = true;
                }
            }
            dirty.clear();

            if (fileHeader.dirty) {
                try {
                    fileHeader.write();
                } catch (Exception e) {
                    error = true;
                }
            }
        }

        if (error) {
            throw new FilerException(FaultCodes.GEN_CRITICAL_ERROR, "Error performing flush!");
	}
    }


    /**
     * createFileHeader must be implemented by a Paged implementation
     * in order to create an appropriate subclass instance of a FileHeader.
     *
     * @return a new FileHeader
     */
    public abstract FileHeader createFileHeader();

    /**
     * createFileHeader must be implemented by a Paged implementation
     * in order to create an appropriate subclass instance of a FileHeader.
     *
     * @param read If true, reads the FileHeader from disk
     * @return a new FileHeader
     * @throws IOException if an exception occurs
     */
    public abstract FileHeader createFileHeader(boolean read) throws IOException;

    /**
     * createFileHeader must be implemented by a Paged implementation
     * in order to create an appropriate subclass instance of a FileHeader.
     *
     * @param pageCount The number of pages to allocate for primary storage
     * @return a new FileHeader
     */
    public abstract FileHeader createFileHeader(long pageCount);

    /**
     * createFileHeader must be implemented by a Paged implementation
     * in order to create an appropriate subclass instance of a FileHeader.
     *
     * @param pageCount The number of pages to allocate for primary storage
     * @param pageSize The size of a Page (should be a multiple of a FS block)
     * @return a new FileHeader
     */
    public abstract FileHeader createFileHeader(long pageCount, int pageSize);

    /**
     * createPageHeader must be implemented by a Paged implementation
     * in order to create an appropriate subclass instance of a PageHeader.
     *
     * @return a new PageHeader
     */
    public abstract PageHeader createPageHeader();


    // These are a bunch of utility methods for subclasses

    public static Value[] insertArrayValue(Value[] vals, Value val, int idx) {
        Value[] newVals = new Value[vals.length + 1];
        if (idx > 0) {
            System.arraycopy(vals, 0, newVals, 0, idx);
	}
        newVals[idx] = val;
        if (idx < vals.length) {
            System.arraycopy(vals, idx, newVals, idx + 1, vals.length - idx);
	}
        return newVals;
    }

    public static Value[] deleteArrayValue(Value[] vals, int idx) {
        Value[] newVals = new Value[vals.length - 1];
        if (idx > 0) {
            System.arraycopy(vals, 0, newVals, 0, idx);
	}
        if (idx < newVals.length) {
            System.arraycopy(vals, idx + 1, newVals, idx, newVals.length - idx);
	}
        return newVals;
    }

    public static long[] insertArrayLong(long[] vals, long val, int idx) {
        long[] newVals = new long[vals.length + 1];
        if (idx > 0) {
            System.arraycopy(vals, 0, newVals, 0, idx);
	}
        newVals[idx] = val;
        if (idx < vals.length) {
            System.arraycopy(vals, idx, newVals, idx + 1, vals.length - idx);
	}
        return newVals;
    }

    public static long[] deleteArrayLong(long[] vals, int idx) {
        long[] newVals = new long[vals.length - 1];
        if (idx > 0) {
            System.arraycopy(vals, 0, newVals, 0, idx);
	}
        if (idx < newVals.length) {
            System.arraycopy(vals, idx + 1, newVals, idx, newVals.length - idx);
	}
        return newVals;
    }

    public static int[] insertArrayInt(int[] vals, int val, int idx) {
        int[] newVals = new int[vals.length + 1];
        if (idx > 0) {
            System.arraycopy(vals, 0, newVals, 0, idx);
	}
        newVals[idx] = val;
        if (idx < vals.length) {
            System.arraycopy(vals, idx, newVals, idx + 1, vals.length - idx);
	}
        return newVals;
    }

    public static int[] deleteArrayInt(int[] vals, int idx) {
        int[] newVals = new int[vals.length - 1];
        if (idx > 0) {
            System.arraycopy(vals, 0, newVals, 0, idx);
	}
        if (idx < newVals.length) {
            System.arraycopy(vals, idx + 1, newVals, idx, newVals.length - idx);
	}
        return newVals;
    }

    public static short[] insertArrayShort(short[] vals, short val, int idx) {
        short[] newVals = new short[vals.length + 1];
        if (idx > 0) {
            System.arraycopy(vals, 0, newVals, 0, idx);
	}
        newVals[idx] = val;
        if (idx < vals.length) {
            System.arraycopy(vals, idx, newVals, idx + 1, vals.length - idx);
	}

        return newVals;
    }

    public static short[] deleteArrayShort(short[] vals, int idx) {
        short[] newVals = new short[vals.length - 1];
        if (idx > 0) {
            System.arraycopy(vals, 0, newVals, 0, idx);
	}
        if (idx < newVals.length) {
            System.arraycopy(vals, idx + 1, newVals, idx, newVals.length - idx);
	}

        return newVals;
    }


    /**
     * FileHeader
     */

    public abstract class FileHeader {
        private boolean dirty = false;
        private int workSize;

        private short headerSize;
        private int pageSize;
        private long pageCount;
        private long totalCount;
        private long firstFreePage = -1;
        private long lastFreePage = -1;
        private byte pageHeaderSize = 64;
        private short maxKeySize = 256;
        private long recordCount;

        public FileHeader() {
            this(1024);
        }

        public FileHeader(long pageCount) {
            this(pageCount, 4096);
        }

        public FileHeader(long pageCount, int pageSize) {
            this.pageSize = pageSize;
            this.pageCount = pageCount;
            totalCount = pageCount;
            headerSize = (short) 4096;
            calculateWorkSize();
        }

        public FileHeader(boolean read) throws IOException {
            if (read) {
                read();
            }
        }

        public synchronized final void read() throws IOException {
            RandomAccessFile raf = null;
            try {
                raf = getDescriptor();
                raf.seek(0);
                read(raf);
                calculateWorkSize();
            } finally {
                putDescriptor(raf);
            }
        }

        public synchronized void read(RandomAccessFile raf) throws IOException {
            headerSize = raf.readShort();
            pageSize = raf.readInt();
            pageCount = raf.readLong();
            totalCount = raf.readLong();
            firstFreePage = raf.readLong();
            lastFreePage = raf.readLong();
            pageHeaderSize = raf.readByte();
            maxKeySize = raf.readShort();
            recordCount = raf.readLong();
        }

        public synchronized final void write() throws IOException {
            if (!dirty) {
                return;
            }

            RandomAccessFile raf = null;
            try {
                raf = getDescriptor();
                raf.seek(0);
                write(raf);
                dirty = false;
            } finally {
                putDescriptor(raf);
            }
        }

        public synchronized void write(RandomAccessFile raf) throws IOException {
            raf.writeShort(headerSize);
            raf.writeInt(pageSize);
            raf.writeLong(pageCount);
            raf.writeLong(totalCount);
            raf.writeLong(firstFreePage);
            raf.writeLong(lastFreePage);
            raf.writeByte(pageHeaderSize);
            raf.writeShort(maxKeySize);
            raf.writeLong(recordCount);
        }

        public synchronized final void setDirty() {
            dirty = true;
        }

        public synchronized final boolean isDirty() {
            return dirty;
        }

        /** The size of the FileHeader.  Usually 1 OS Page */
        public synchronized final void setHeaderSize(short headerSize) {
            this.headerSize = headerSize;
            dirty = true;
        }

        /** The size of the FileHeader.  Usually 1 OS Page */
        public synchronized final short getHeaderSize() {
            return headerSize;
        }

        /** The size of a page.  Usually a multiple of a FS block */
        public synchronized final void setPageSize(int pageSize) {
            this.pageSize = pageSize;
            calculateWorkSize();
            dirty = true;
        }

        /** The size of a page.  Usually a multiple of a FS block */
        public synchronized final int getPageSize() {
            return pageSize;
        }

        /** The number of pages in primary storage */
        public synchronized final void setPageCount(long pageCount) {
            this.pageCount = pageCount;
            dirty = true;
        }

        /** The number of pages in primary storage */
        public synchronized final long getPageCount() {
            return pageCount;
        }

        /** The number of total pages in the file */
        public synchronized final void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
            dirty = true;
        }

        /** The number of total pages in the file */
        public synchronized final long getTotalCount() {
            return totalCount;
        }

        /** The first free page in unused secondary space */
        public synchronized final void setFirstFreePage(long firstFreePage) {
            this.firstFreePage = firstFreePage;
            dirty = true;
        }

        /** The first free page in unused secondary space */
        public synchronized final long getFirstFreePage() {
            return firstFreePage;
        }

        /** The last free page in unused secondary space */
        public synchronized final void setLastFreePage(long lastFreePage) {
            this.lastFreePage = lastFreePage;
            dirty = true;
        }

        /** The last free page in unused secondary space */
        public synchronized final long getLastFreePage() {
            return lastFreePage;
        }

        /**
        * Set the size of a page header.
        *
        * Normally, 64 is sufficient.
        */
        public synchronized final void setPageHeaderSize(byte pageHeaderSize) {
            this.pageHeaderSize = pageHeaderSize;
            calculateWorkSize();
            dirty = true;
        }

        /**
        * Get the size of a page header.
        *
        * Normally, 64 is sufficient
        */
        public synchronized final byte getPageHeaderSize() {
            return pageHeaderSize;
        }

        /**
        * Set the maximum number of bytes a key can be.
        *
        * Normally, 256 is good
        */
        public synchronized final void setMaxKeySize(short maxKeySize) {
            this.maxKeySize = maxKeySize;
            dirty = true;
        }

        /**
        * Get the maximum number of bytes.
        *
        * Normally, 256 is good.
        */
        public synchronized final short getMaxKeySize() {
            return maxKeySize;
        }

        /** The number of records being managed by the file (not pages) */
        public synchronized final void setRecordCount(long recordCount) {
            this.recordCount = recordCount;
            dirty = true;
        }

        /** Increment the number of records being managed by the file */
        public synchronized final void incRecordCount() {
            recordCount++;
            dirty = true;
        }

        /** Decrement the number of records being managed by the file */
        public synchronized final void decRecordCount() {
            recordCount--;
            dirty = true;
        }

        /** The number of records being managed by the file (not pages) */
        public synchronized final long getRecordCount() {
            return recordCount;
        }

        private synchronized void calculateWorkSize() {
            workSize = pageSize - pageHeaderSize;
        }

        public synchronized final int getWorkSize() {
            return workSize;
        }
    }

    /**
     * PageHeader
     */

    public abstract class PageHeader implements Streamable {
        private boolean dirty = false;
        private byte status = UNUSED;
        private int keyLen = 0;
        private int keyHash = 0;
        private int dataLen = 0;
        private int recordLen = 0;
        private long nextPage = -1;

        public PageHeader() {
        }

        public PageHeader(DataInputStream dis) throws IOException {
            read(dis);
        }

        public synchronized void read(DataInputStream dis) throws IOException {
            status = dis.readByte();
            dirty = false;

            if (status == UNUSED) {
                return;
            }

            keyLen = dis.readInt();
            if (keyLen < 0) {
                // hack for win98/ME - see issue 564
                keyLen = 0;
            }
            keyHash = dis.readInt();
            dataLen = dis.readInt();
            recordLen = dis.readInt();
            nextPage = dis.readLong();
        }

        public synchronized void write(DataOutputStream dos) throws IOException {
            dirty = false;
            dos.writeByte(status);
            dos.writeInt(keyLen);
            dos.writeInt(keyHash);
            dos.writeInt(dataLen);
            dos.writeInt(recordLen);
            dos.writeLong(nextPage);
        }

        public synchronized final boolean isDirty() {
            return dirty;
        }

        public synchronized final void setDirty() {
            dirty = true;
        }

        /** The status of this page (UNUSED, RECORD, DELETED, etc...) */
        public synchronized final void setStatus(byte status) {
            this.status = status;
            dirty = true;
        }

        /** The status of this page (UNUSED, RECORD, DELETED, etc...) */
        public synchronized final byte getStatus() {
            return status;
        }

        public synchronized final void setKey(Key key) {
            // setKey WIPES OUT the Page data
            setRecordLen(0);
            dataLen = 0;
            keyHash = key.getHash();
            keyLen = key.getLength();
            dirty = true;
        }

        /** The length of the Key */
        public synchronized final void setKeyLen(int keyLen) {
            this.keyLen = keyLen;
            dirty = true;
        }

        /** The length of the Key */
        public synchronized final int getKeyLen() {
            return keyLen;
        }

        /** The hashed value of the Key for quick comparisons */
        public synchronized final void setKeyHash(int keyHash) {
            this.keyHash = keyHash;
            dirty = true;
        }

        /** The hashed value of the Key for quick comparisons */
        public synchronized final int getKeyHash() {
            return keyHash;
        }

        /** The length of the Data */
        public synchronized final void setDataLen(int dataLen) {
            this.dataLen = dataLen;
            dirty = true;
        }

        /** The length of the Data */
        public synchronized final int getDataLen() {
            return dataLen;
        }

        /** The length of the Record's value */
        public synchronized void setRecordLen(int recordLen) {
            this.recordLen = recordLen;
            dirty = true;
        }

        /** The length of the Record's value */
        public synchronized final int getRecordLen() {
            return recordLen;
        }

        /** The next page for this Record (if overflowed) */
        public synchronized final void setNextPage(long nextPage) {
            this.nextPage = nextPage;
            dirty = true;
        }

        /** The next page for this Record (if overflowed) */
        public synchronized final long getNextPage() {
            return nextPage;
        }
    }

    /**
     * Page
     */

    public final class Page implements Comparable {
        /** This page number */
        private long pageNum;

        /** The data for this page */
        private byte[] data;

        /** The Header for this Page */
        private PageHeader header = createPageHeader();

        /** The position (relative) of the Key in the data array */
        private int keyPos;

        /** The position (relative) of the Data in the data array */
        private int dataPos;

        /** The offset into the file that this page starts */
        private long offset;

        private boolean loaded;

        public Page() {
        }

        public Page(long pageNum) throws IOException {
            this();
            setPageNum(pageNum);
        }

        public synchronized void read() throws IOException {
            RandomAccessFile raf = null;
            try {
                data = new byte[fileHeader.pageSize];

                raf = getDescriptor();
                raf.seek(offset);
                raf.read(data);

                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bis);

                // Read in the header
                header.read(dis);

                keyPos = fileHeader.pageHeaderSize;
                dataPos = keyPos + header.keyLen;
            } finally {
                putDescriptor(raf);
            }
        }

        public synchronized void write() throws IOException {
            // Write out the header
            ByteArrayOutputStream bos = new ByteArrayOutputStream(fileHeader.getPageHeaderSize());
            DataOutputStream dos = new DataOutputStream(bos);
            header.write(dos);
            byte[] b = bos.toByteArray();
            System.arraycopy(b, 0, data, 0, b.length);

            dirty.put(new Long(pageNum), this);
            if (dirty.size() > MAX_DIRTY_SIZE) {
                try {
                    // Too many dirty pages... flush them
                    Paged.this.flush();
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }
        }

        public synchronized void flush() throws IOException {
            RandomAccessFile raf = null;
            try {
                raf = getDescriptor();
                if (offset >= raf.length()) {
                    // Grow the file
                    long o = (fileHeader.headerSize + ((fileHeader.totalCount * 3) / 2) * fileHeader.pageSize) + (fileHeader.pageSize - 1);
                    raf.seek(o);
                    raf.writeByte(0);
                }
                raf.seek(offset);
                raf.write(data);
                if (sync) {
                    raf.getFD().sync();
                }
            } finally {
                putDescriptor(raf);
            }
        }

        public synchronized void setPageNum(long pageNum) {
            this.pageNum = pageNum;
            offset = fileHeader.headerSize + (pageNum * fileHeader.pageSize);
        }

        public synchronized long getPageNum() {
            return pageNum;
        }

        public synchronized PageHeader getPageHeader() {
            return header;
        }

        public synchronized void setKey(Key key) {
            header.setKey(key);
            // Insert the key into the data array.
            key.copyTo(data, keyPos);

            // Set the start of data to skip over the key.
            dataPos = keyPos + header.keyLen;
        }

        public synchronized Key getKey() {
            if (header.keyLen > 0) {
                return new Key(data, keyPos, header.keyLen);
            } else {
                return null;
            }
        }

        public synchronized void streamTo(OutputStream os) throws IOException {
            if (header.dataLen > 0) {
                os.write(data, dataPos, header.dataLen);
            }
        }

        public synchronized void streamFrom(InputStream is) throws IOException {
            int avail = is.available();
            header.dataLen = fileHeader.workSize - header.keyLen;
            if (avail < header.dataLen) {
                header.dataLen = avail;
            }
            if (header.dataLen > 0) {
                is.read(data, keyPos + header.keyLen, header.dataLen);
            }
        }

        public synchronized boolean isLoaded() {
            return loaded;
        }

        public synchronized void setLoaded(boolean loaded) {
            this.loaded = loaded;
        }

        public synchronized int compareTo(Object o) {
            return (int) (pageNum - ((Page) o).pageNum);
        }
    }
}

