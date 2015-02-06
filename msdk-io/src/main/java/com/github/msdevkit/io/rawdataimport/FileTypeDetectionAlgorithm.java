/* 
 * Copyright 2015 MSDK Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.github.msdevkit.io.rawdataimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.msdevkit.MSDKAlgorithm;
import com.github.msdevkit.MSDKException;
import com.github.msdevkit.datamodel.RawDataFileType;

/**
 * Detector of raw data file format
 */
public class FileTypeDetectionAlgorithm implements
	MSDKAlgorithm<RawDataFileType> {

    /*
     * See
     * "http://www.unidata.ucar.edu/software/netcdf/docs/netcdf/File-Format-Specification.html"
     */
    private static final String CDF_HEADER = "CDF";

    /*
     * mzML files with index start with <indexedmzML><mzML>tags, but files with
     * no index contain only the <mzML> tag. See
     * "http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/schema/mzML1.1.0.xsd"
     */
    private static final String MZML_HEADER = "<mzML";

    /*
     * mzXML files with index start with <mzXML><msRun> tags, but files with no
     * index contain only the <msRun> tag. See
     * "http://sashimi.sourceforge.net/schema_revision/mzXML_3.2/mzXML_3.2.xsd"
     */
    private static final String MZXML_HEADER = "<msRun";

    // See "http://www.psidev.info/sites/default/files/mzdata.xsd.txt"
    private static final String MZDATA_HEADER = "<mzData";

    // See "https://code.google.com/p/unfinnigan/wiki/FileHeader"
    private static final String THERMO_HEADER = String.valueOf(new char[] {
	    0x01, 0xA1, 'F', 0, 'i', 0, 'n', 0, 'n', 0, 'i', 0, 'g', 0, 'a', 0,
	    'n', 0 });

    private @Nonnull File fileName;
    private @Nullable RawDataFileType result = null;
    private double finishedPercentage = 0.0;

    /**
     * 
     * @return Detected file type or null if the file is not of any supported
     *         type
     */
    public FileTypeDetectionAlgorithm(@Nonnull File fileName) {
	this.fileName = fileName;
    }

    @Override
    public void execute() throws MSDKException {

	try {
	    result = detectDataFileType(fileName);
	} catch (IOException e) {
	    throw new MSDKException(e);
	}
	finishedPercentage = 1.0;
    }

    private RawDataFileType detectDataFileType(File fileName)
	    throws IOException {

	if (fileName.isDirectory()) {
	    // To check for Waters .raw directory, we look for _FUNC[0-9]{3}.DAT
	    for (File f : fileName.listFiles()) {
		if (f.isFile() && f.getName().matches("_FUNC[0-9]{3}.DAT"))
		    return RawDataFileType.WATERS_RAW;
	    }
	    // We don't recognize any other directory type than Waters
	    return RawDataFileType.UNSUPPORTED;
	}

	if (fileName.getName().toLowerCase().endsWith(".csv")) {
	    return RawDataFileType.AGILENT_CSV;
	}

	// Read the first 1kB of the file into a String
	InputStreamReader reader = new InputStreamReader(new FileInputStream(
		fileName), "ISO-8859-1");
	char buffer[] = new char[1024];
	reader.read(buffer);
	reader.close();
	String fileHeader = new String(buffer);

	if (fileHeader.startsWith(THERMO_HEADER)) {
	    return RawDataFileType.THERMO_RAW;
	}

	if (fileHeader.startsWith(CDF_HEADER)) {
	    return RawDataFileType.NETCDF;
	}

	if (fileHeader.contains(MZML_HEADER))
	    return RawDataFileType.MZML;

	if (fileHeader.contains(MZDATA_HEADER))
	    return RawDataFileType.MZDATA;

	if (fileHeader.contains(MZXML_HEADER))
	    return RawDataFileType.MZXML;

	return RawDataFileType.UNSUPPORTED;

    }

    @Override
    public double getFinishedPercentage() {
	return finishedPercentage;
    }

    @Override
    @Nullable
    public RawDataFileType getResult() {
	return result;
    }

    @Override
    public void cancel() {
	// This method is too fast to be canceled
    }

}