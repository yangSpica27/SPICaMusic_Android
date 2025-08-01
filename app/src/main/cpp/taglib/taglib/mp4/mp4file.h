/**************************************************************************
    copyright            : (C) 2007 by Lukáš Lalinský
    email                : lalinsky@gmail.com
 **************************************************************************/

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

#ifndef TAGLIB_MP4FILE_H
#define TAGLIB_MP4FILE_H

#include "tfile.h"
#include "taglib_export.h"
#include "mp4tag.h"
#include "tag.h"
#include "mp4properties.h"

namespace TagLib {
  //! An implementation of MP4 (AAC, ALAC, ...) metadata
  namespace MP4 {
    class Atoms;
    class ItemFactory;


    /*!
     * This implements and provides an interface for MP4 files to the
     * TagLib::Tag and TagLib::AudioProperties interfaces by way of implementing
     * the abstract TagLib::File API as well as providing some additional
     * information specific to MP4 files.
     */
    class TAGLIB_EXPORT File : public TagLib::File
    {
    public:
      /*!
       * This set of flags is used for strip() and is suitable for
       * being OR-ed together.
       */
      enum TagTypes {
        //! Empty set.  Matches no tag types.
        NoTags  = 0x0000,
        //! Matches MP4 tags.
        MP4     = 0x0001,
        //! Matches all tag types.
        AllTags = 0xffff
      };

      /*!
       * Constructs an MP4 file from \a file.  If \a readProperties is true the
       * file's audio properties will also be read.
       *
       * \note In the current implementation, \a propertiesStyle is ignored.
       *
       * The items will be created using \a itemFactory (default if null).
       */
      File(FileName file, bool readProperties = true,
           Properties::ReadStyle audioPropertiesStyle = Properties::Average,
           ItemFactory *itemFactory = nullptr);

      /*!
       * Constructs an MP4 file from \a stream.  If \a readProperties is true the
       * file's audio properties will also be read.
       *
       * \note TagLib will *not* take ownership of the stream, the caller is
       * responsible for deleting it after the File object.
       *
       * \note In the current implementation, \a propertiesStyle is ignored.
       *
       * The items will be created using \a itemFactory (default if null).
       */
      File(IOStream *stream, bool readProperties = true,
           Properties::ReadStyle audioPropertiesStyle = Properties::Average,
           ItemFactory *itemFactory = nullptr);

      /*!
       * Destroys this instance of the File.
       */
      ~File() override;

      File(const File &) = delete;
      File &operator=(const File &) = delete;

      /*!
       * Returns a pointer to the MP4 tag of the file.
       *
       * MP4::Tag implements the tag interface, so this serves as the
       * reimplementation of TagLib::File::tag().
       *
       * \note The Tag <b>is still</b> owned by the MP4::File and should not be
       * deleted by the user.  It will be deleted when the file (object) is
       * destroyed.
       */
      Tag *tag() const override;

      /*!
       * Implements the unified property interface -- export function.
       */
      PropertyMap properties() const override;

      /*!
       * Removes unsupported properties. Forwards to the actual Tag's
       * removeUnsupportedProperties() function.
       */
      void removeUnsupportedProperties(const StringList &properties) override;

      /*!
       * Implements the unified property interface -- import function.
       */
      PropertyMap setProperties(const PropertyMap &) override;

      /*!
       * Returns the MP4 audio properties for this file.
       */
      Properties *audioProperties() const override;

      /*!
       * Save the file.
       *
       * This returns true if the save was successful.
       */
      bool save() override;

      /*!
       * This will strip the tags that match the OR-ed together TagTypes from the
       * file.  By default it strips all tags.  It returns true if the tags are
       * successfully stripped.
       *
       * \note This will update the file immediately.
       */
      bool strip(int tags = AllTags);

      /*!
       * Returns whether or not the file on disk actually has an MP4 tag, or the
       * file has a Metadata Item List (ilst) atom.
       */
      bool hasMP4Tag() const;

      /*!
       * Returns whether or not the given \a stream can be opened as an ASF
       * file.
       *
       * \note This method is designed to do a quick check.  The result may
       * not necessarily be correct.
       */
      static bool isSupported(IOStream *stream);

    private:
      void read(bool readProperties);

      class FilePrivate;
      TAGLIB_MSVC_SUPPRESS_WARNING_NEEDS_TO_HAVE_DLL_INTERFACE
      std::unique_ptr<FilePrivate> d;
    };
  }  // namespace MP4
}  // namespace TagLib
#endif
