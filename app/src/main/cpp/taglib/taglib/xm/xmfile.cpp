/***************************************************************************
    copyright           : (C) 2011 by Mathias Panzenböck
    email               : grosser.meister.morti@gmx.net
 ***************************************************************************/

/***************************************************************************
 *   This library is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Lesser General Public License version   *
 *   2.1 as published by the Free Software Foundation.                     *
 *                                                                         *
 *   This library is distributed in the hope that it will be useful, but   *
 *   WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU     *
 *   Lesser General Public License for more details.                       *
 *                                                                         *
 *   You should have received a copy of the GNU Lesser General Public      *
 *   License along with this library; if not, write to the Free Software   *
 *   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA         *
 *   02110-1301  USA                                                       *
 *                                                                         *
 *   Alternatively, this file is available under the Mozilla Public        *
 *   License Version 1.1.  You may obtain a copy of the License at         *
 *   http://www.mozilla.org/MPL/                                           *
 ***************************************************************************/

#include "xmfile.h"

#include <algorithm>
#include <utility>
#include <numeric>

#include "tstringlist.h"
#include "tdebug.h"
#include "tpropertymap.h"
#include "modfileprivate.h"

using namespace TagLib;
using namespace XM;

namespace
{

/*!
 * The Reader classes are helpers to make handling of the stripped XM
 * format more easy. In the stripped XM format certain header sizes might
 * be smaller than one would expect. The fields that are not included
 * are then just some predefined valued (e.g. 0).
 *
 * Using these classes this code:
 *
 *   if(headerSize >= 4) {
 *     if(!readU16L(value1)) ERROR();
 *     if(headerSize >= 8) {
 *       if(!readU16L(value2)) ERROR();
 *       if(headerSize >= 12) {
 *         if(!readString(value3, 22)) ERROR();
 *         ...
 *       }
 *     }
 *   }
 *
 * Becomes:
 *
 *   StructReader header;
 *   header.u16L(value1).u16L(value2).string(value3, 22). ...;
 *   if(header.read(*this, headerSize) < std::min(header.size(), headerSize))
 *     ERROR();
 *
 * Maybe if this is useful to other formats these classes can be moved to
 * their own public files.
 */
class Reader
{
public:
  virtual ~Reader() = default;

  /*!
   * Reads associated values from \a file, but never reads more
   * then \a limit bytes.
   */
  virtual unsigned int read(TagLib::File &file, unsigned int limit) = 0;

  /*!
   * Returns the number of bytes this reader would like to read.
   */
  virtual unsigned int size() const = 0;
};

class SkipReader : public Reader
{
public:
  SkipReader(unsigned int size) : m_size(size)
  {
  }

  unsigned int read(TagLib::File &file, unsigned int limit) override
  {
    unsigned int count = std::min(m_size, limit);
    file.seek(count, TagLib::File::Current);
    return count;
  }

  unsigned int size() const override
  {
    return m_size;
  }

private:
  unsigned int m_size;
};

template<typename T>
class ValueReader : public Reader
{
public:
  ValueReader(T &value) : value(value)
  {
  }

protected:
  T &value;
};

class StringReader : public ValueReader<String>
{
public:
  StringReader(String &string, unsigned int size) :
    ValueReader<String>(string), m_size(size)
  {
  }

  unsigned int read(TagLib::File &file, unsigned int limit) override
  {
    ByteVector data = file.readBlock(std::min(m_size, limit));
    unsigned int count = data.size();
    int index = data.find(static_cast<char>(0));
    if(index > -1) {
      data.resize(index);
    }
    data.replace('\xff', ' ');
    value = data;
    return count;
  }

  unsigned int size() const override
  {
    return m_size;
  }

private:
  unsigned int m_size;
};

class ByteReader : public ValueReader<unsigned char>
{
public:
  using ValueReader::ValueReader;
  unsigned int read(TagLib::File &file, unsigned int limit) override
  {
    ByteVector data = file.readBlock(std::min(1U,limit));
    if(!data.isEmpty()) {
      value = data[0];
    }
    return data.size();
  }

  unsigned int size() const override
  {
    return 1;
  }
};

template<typename T>
class NumberReader : public ValueReader<T>
{
public:
  NumberReader(T &value, bool bigEndian) :
    ValueReader<T>(value), bigEndian(bigEndian)
  {
  }

protected:
  bool bigEndian;
};

class U16Reader : public NumberReader<unsigned short>
{
public:
  U16Reader(unsigned short &value, bool bigEndian)
  : NumberReader<unsigned short>(value, bigEndian) {}

  unsigned int read(TagLib::File &file, unsigned int limit) override
  {
    ByteVector data = file.readBlock(std::min(2U,limit));
    value = data.toUShort(bigEndian);
    return data.size();
  }

  unsigned int size() const override
  {
    return 2;
  }
};

class U32Reader : public NumberReader<unsigned long>
{
public:
  U32Reader(unsigned long &value, bool bigEndian = true) :
    NumberReader<unsigned long>(value, bigEndian)
  {
  }

  unsigned int read(TagLib::File &file, unsigned int limit) override
  {
    ByteVector data = file.readBlock(std::min(4U,limit));
    value = data.toUInt(bigEndian);
    return data.size();
  }

  unsigned int size() const override
  {
    return 4;
  }
};

class StructReader : public Reader
{
public:
  /*!
   * Add a nested reader. This reader takes ownership.
   */
  StructReader &reader(std::unique_ptr<Reader> reader)
  {
    m_readers.push_back(std::move(reader));
    return *this;
  }

  /*!
   * Don't read anything but skip \a size bytes.
   */
  StructReader &skip(unsigned int size)
  {
    m_readers.push_back(std::make_unique<SkipReader>(size));
    return *this;
  }

  /*!
   * Read a string of \a size characters (bytes) into \a string.
   */
  StructReader &string(String &string, unsigned int size)
  {
    m_readers.push_back(std::make_unique<StringReader>(string, size));
    return *this;
  }

  /*!
   * Read a byte into \a byte.
   */
  StructReader &byte(unsigned char &byte)
  {
    m_readers.push_back(std::make_unique<ByteReader>(byte));
    return *this;
  }

  /*!
   * Read a unsigned 16 Bit integer into \a number. The byte order
   * is controlled by \a bigEndian.
   */
  StructReader &u16(unsigned short &number, bool bigEndian)
  {
    m_readers.push_back(std::make_unique<U16Reader>(number, bigEndian));
    return *this;
  }

  /*!
   * Read a unsigned 16 Bit little endian integer into \a number.
   */
  StructReader &u16L(unsigned short &number)
  {
    return u16(number, false);
  }

  /*!
   * Read a unsigned 16 Bit big endian integer into \a number.
   */
  StructReader &u16B(unsigned short &number)
  {
    return u16(number, true);
  }

  /*!
   * Read a unsigned 32 Bit integer into \a number. The byte order
   * is controlled by \a bigEndian.
   */
  StructReader &u32(unsigned long &number, bool bigEndian)
  {
    m_readers.push_back(std::make_unique<U32Reader>(number, bigEndian));
    return *this;
  }

  /*!
   * Read a unsigned 32 Bit little endian integer into \a number.
   */
  StructReader &u32L(unsigned long &number)
  {
    return u32(number, false);
  }

  /*!
   * Read a unsigned 32 Bit big endian integer into \a number.
   */
  StructReader &u32B(unsigned long &number)
  {
    return u32(number, true);
  }

  unsigned int size() const override
  {
    return std::accumulate(m_readers.cbegin(), m_readers.cend(), 0U,
        [](unsigned int acc, const auto &rdr) {
      return acc + rdr->size();
    });
  }

  unsigned int read(TagLib::File &file, unsigned int limit) override
  {
    unsigned int sumcount = 0;
    for(const auto &rdr : std::as_const(m_readers)) {
      if(limit == 0)
        break;
      unsigned int count = rdr->read(file, limit);
      limit    -= count;
      sumcount += count;
    }
    return sumcount;
  }

private:
  std::list<std::unique_ptr<Reader>> m_readers;
};

} // namespace

class XM::File::FilePrivate
{
public:
  FilePrivate(AudioProperties::ReadStyle propertiesStyle)
    :  properties(propertiesStyle)
  {
  }

  Mod::Tag       tag;
  XM::Properties properties;
};

XM::File::File(FileName file, bool readProperties,
               AudioProperties::ReadStyle propertiesStyle) :
  Mod::FileBase(file),
  d(std::make_unique<FilePrivate>(propertiesStyle))
{
  if(isOpen())
    read(readProperties);
}

XM::File::File(IOStream *stream, bool readProperties,
               AudioProperties::ReadStyle propertiesStyle) :
  Mod::FileBase(stream),
  d(std::make_unique<FilePrivate>(propertiesStyle))
{
  if(isOpen())
    read(readProperties);
}

XM::File::~File() = default;

Mod::Tag *XM::File::tag() const
{
  return &d->tag;
}

PropertyMap XM::File::properties() const
{
  return d->tag.properties();
}

PropertyMap XM::File::setProperties(const PropertyMap &properties)
{
  return d->tag.setProperties(properties);
}

XM::Properties *XM::File::audioProperties() const
{
  return &d->properties;
}

bool XM::File::save()
{
  if(readOnly()) {
    debug("XM::File::save() - Cannot save to a read only file.");
    return false;
  }

  seek(17);
  writeString(d->tag.title(), 20);

  seek(38);
  writeString(d->tag.trackerName(), 20);

  seek(60);
  unsigned long headerSize = 0;
  if(!readU32L(headerSize))
    return false;

  seek(70);
  unsigned short patternCount = 0;
  unsigned short instrumentCount = 0;
  if(!readU16L(patternCount) || !readU16L(instrumentCount))
    return false;

  offset_t pos = 60 + headerSize;

  // need to read patterns again in order to seek to the instruments:
  for(unsigned short i = 0; i < patternCount; ++ i) {
    seek(pos);
    unsigned long patternHeaderLength = 0;
    if(!readU32L(patternHeaderLength) || patternHeaderLength < 4)
      return false;

    seek(pos + 7);
    unsigned short dataSize = 0;
    if (!readU16L(dataSize))
      return false;

    pos += patternHeaderLength + dataSize;
  }

  const StringList lines = d->tag.comment().split("\n");
  unsigned int sampleNameIndex = instrumentCount;
  for(unsigned short i = 0; i < instrumentCount; ++ i) {
    seek(pos);
    unsigned long instrumentHeaderSize = 0;
    if(!readU32L(instrumentHeaderSize) || instrumentHeaderSize < 4)
      return false;

    seek(pos + 4);
    const auto len = std::min(22UL, instrumentHeaderSize - 4);
    if(i >= lines.size())
      writeString(String(), len);
    else
      writeString(lines[i], len);

    unsigned short sampleCount = 0;
    if(instrumentHeaderSize >= 29U) {
      seek(pos + 27);
      if(!readU16L(sampleCount))
        return false;
    }

    unsigned long sampleHeaderSize = 0;
    if(sampleCount > 0) {
      seek(pos + 29);
      if(instrumentHeaderSize < 33U || !readU32L(sampleHeaderSize))
        return false;
    }

    pos += instrumentHeaderSize;

    for(unsigned short j = 0; j < sampleCount; ++ j) {
      if(sampleHeaderSize > 4U) {
        seek(pos);
        unsigned long sampleLength = 0;
        if(!readU32L(sampleLength))
          return false;

        if(sampleHeaderSize > 18U) {
          seek(pos + 18);
          const auto sz = std::min(sampleHeaderSize - 18, 22UL);
          if(sampleNameIndex >= lines.size())
            writeString(String(), sz);
          else
            writeString(lines[sampleNameIndex ++], sz);
        }
      }
      pos += sampleHeaderSize;
    }
  }

  return true;
}

void XM::File::read(bool)
{
  if(!isOpen())
    return;

  seek(0);
  ByteVector magic = readBlock(17);
  // it's all 0x00 for stripped XM files:
  READ_ASSERT(magic == "Extended Module: " || magic == ByteVector(17, 0));

  READ_STRING(d->tag.setTitle, 20);
  READ_BYTE_AS(escape);
  // in stripped XM files this is 0x00:
  READ_ASSERT(escape == 0x1A || escape == 0x00);

  READ_STRING(d->tag.setTrackerName, 20);
  READ_U16L(d->properties.setVersion);

  READ_U32L_AS(headerSize);
  READ_ASSERT(headerSize >= 4);

  unsigned short length          = 0;
  unsigned short restartPosition = 0;
  unsigned short channels        = 0;
  unsigned short patternCount    = 0;
  unsigned short instrumentCount = 0;
  unsigned short flags    = 0;
  unsigned short tempo    = 0;
  unsigned short bpmSpeed = 0;

  StructReader header;
  header.u16L(length)
        .u16L(restartPosition)
        .u16L(channels)
        .u16L(patternCount)
        .u16L(instrumentCount)
        .u16L(flags)
        .u16L(tempo)
        .u16L(bpmSpeed);

  unsigned int count = header.read(*this, static_cast<unsigned int>(headerSize - 4));
  unsigned int size = std::min(static_cast<unsigned int>(headerSize - 4), header.size());

  READ_ASSERT(count == size);

  d->properties.setLengthInPatterns(length);
  d->properties.setRestartPosition(restartPosition);
  d->properties.setChannels(channels);
  d->properties.setPatternCount(patternCount);
  d->properties.setInstrumentCount(instrumentCount);
  d->properties.setFlags(flags);
  d->properties.setTempo(tempo);
  d->properties.setBpmSpeed(bpmSpeed);

  seek(60 + headerSize);

  // read patterns:
  for(unsigned short i = 0; i < patternCount; ++ i) {
    READ_U32L_AS(patternHeaderLength);
    READ_ASSERT(patternHeaderLength >= 4);

    unsigned char  packingType = 0;
    unsigned short rowCount = 0;
    unsigned short dataSize = 0;
    StructReader pattern;
    pattern.byte(packingType).u16L(rowCount).u16L(dataSize);

    unsigned int ptCnt = pattern.read(*this, static_cast<unsigned int>(patternHeaderLength - 4));
    READ_ASSERT(ptCnt == std::min(patternHeaderLength - 4U,
                                  static_cast<unsigned long>(pattern.size())));

    seek(patternHeaderLength - (4 + ptCnt) + dataSize, Current);
  }

  StringList instrumentNames;
  StringList sampleNames;
  unsigned int sumSampleCount = 0;

  // read instruments:
  for(unsigned short i = 0; i < instrumentCount; ++ i) {
    READ_U32L_AS(instrumentHeaderSize);
    READ_ASSERT(instrumentHeaderSize >= 4);

    String instrumentName;
    unsigned char  instrumentType = 0;
    unsigned short sampleCount = 0;

    StructReader instrument;
    instrument.string(instrumentName, 22).byte(instrumentType).u16L(sampleCount);

    // 4 for instrumentHeaderSize
    unsigned int inCnt = 4 + instrument.read(*this, static_cast<unsigned int>(instrumentHeaderSize - 4));
    READ_ASSERT(inCnt == std::min(instrumentHeaderSize,
                                  static_cast<unsigned long>(instrument.size() + 4)));

    offset_t offset = 0;
    if(sampleCount > 0) {
      unsigned long sampleHeaderSize = 0;
      sumSampleCount += sampleCount;
      // wouldn't know which header size to assume otherwise:
      READ_ASSERT(instrumentHeaderSize >= inCnt + 4 && readU32L(sampleHeaderSize));
      // skip unhandled header proportion:
      seek(instrumentHeaderSize - inCnt - 4, Current);

      for(unsigned short j = 0; j < sampleCount; ++ j) {
        unsigned long sampleLength = 0;
        unsigned long loopStart    = 0;
        unsigned long loopLength   = 0;
        unsigned char volume       = 0;
        unsigned char finetune     = 0;
        unsigned char sampleType   = 0;
        unsigned char panning      = 0;
        unsigned char noteNumber   = 0;
        unsigned char compression  = 0;
        String sampleName;
        StructReader sample;
        sample.u32L(sampleLength)
              .u32L(loopStart)
              .u32L(loopLength)
              .byte(volume)
              .byte(finetune)
              .byte(sampleType)
              .byte(panning)
              .byte(noteNumber)
              .byte(compression)
              .string(sampleName, 22);

        unsigned int smCnt = sample.read(*this, static_cast<unsigned int>(sampleHeaderSize));
        READ_ASSERT(smCnt == std::min(sampleHeaderSize,
                                      static_cast<unsigned long>(sample.size())));
        // skip unhandled header proportion:
        seek(sampleHeaderSize - smCnt, Current);

        offset += sampleLength;
        sampleNames.append(sampleName);
      }
    }
    else {
      offset = instrumentHeaderSize - inCnt;
    }
    instrumentNames.append(instrumentName);
    seek(offset, Current);
  }

  d->properties.setSampleCount(sumSampleCount);
  String comment(instrumentNames.toString("\n"));
  if(!sampleNames.isEmpty()) {
    comment += "\n";
    comment += sampleNames.toString("\n");
  }
  d->tag.setComment(comment);
}
