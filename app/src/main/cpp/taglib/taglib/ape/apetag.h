/***************************************************************************
    copyright            : (C) 2004 by Allan Sandfeld Jensen
    email                : kde@carewolf.org
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

#ifndef TAGLIB_APETAG_H
#define TAGLIB_APETAG_H

#include "tbytevector.h"
#include "tmap.h"
#include "tstring.h"
#include "taglib_export.h"
#include "tag.h"
#include "apeitem.h"

namespace TagLib {

  class File;

  //! An implementation of the APE tagging format

  namespace APE {

    class Footer;

    /*!
     * A mapping between a list of item names, or keys, and the associated item.
     *
     * \see APE::Tag::itemListMap()
     */
    using ItemListMap = Map<const String, Item>;

    //! An APE tag implementation

    class TAGLIB_EXPORT Tag : public TagLib::Tag
    {
    public:
      /*!
       * Create an APE tag with default values.
       */
      Tag();

      /*!
       * Create an APE tag and parse the data in \a file with APE footer at
       * \a tagOffset.
       */
      Tag(TagLib::File *file, offset_t footerLocation);

      /*!
       * Destroys this Tag instance.
       */
      ~Tag() override;

      Tag(const Tag &) = delete;
      Tag &operator=(const Tag &) = delete;

      /*!
       * Renders the in memory values to a ByteVector suitable for writing to
       * the file.
       */
      ByteVector render() const;

      /*!
       * Returns the string "APETAGEX" suitable for usage in locating the tag in a
       * file.
       */
      static ByteVector fileIdentifier();

      // Reimplementations.

      String title() const override;
      String artist() const override;
      String album() const override;
      String comment() const override;
      String genre() const override;
      unsigned int year() const override;
      unsigned int track() const override;

      void setTitle(const String &s) override;
      void setArtist(const String &s) override;
      void setAlbum(const String &s) override;
      void setComment(const String &s) override;
      void setGenre(const String &s) override;
      void setYear(unsigned int i) override;
      void setTrack(unsigned int i) override;

      /*!
       * Implements the unified tag dictionary interface -- export function.
       * APE tags are perfectly compatible with the dictionary interface because they
       * support both arbitrary tag names and multiple values. Currently only
       * APE items of type *Text* are handled by the dictionary interface; all *Binary*
       * and *Locator* items will be put into the unsupportedData list and can be
       * deleted on request using removeUnsupportedProperties(). The same happens
       * to Text items if their key is invalid for PropertyMap (which should actually
       * never happen).
       *
       * The only conversion done by this export function is to rename the APE tags
       * TRACK to TRACKNUMBER, YEAR to DATE, and ALBUM ARTIST to ALBUMARTIST, respectively,
       * in order to be compliant with the names used in other formats.
       */
      PropertyMap properties() const override;

      void removeUnsupportedProperties(const StringList &properties) override;

      /*!
       * Implements the unified tag dictionary interface -- import function. The same
       * comments as for the export function apply; additionally note that the APE tag
       * specification requires keys to have between 2 and 16 printable ASCII characters
       * with the exception of the fixed strings "ID3", "TAG", "OGGS", and "MP+".
       */
      PropertyMap setProperties(const PropertyMap &) override;

      StringList complexPropertyKeys() const override;
      List<VariantMap> complexProperties(const String &key) const override;
      bool setComplexProperties(const String &key, const List<VariantMap> &value) override;

      /*!
       * Check if the given String is a valid APE tag key.
       */
      static bool checkKey(const String&);

      /*!
       * Returns a pointer to the tag's footer.
       */
      Footer *footer() const;

      /*!
       * Returns a reference to the item list map.  This is an ItemListMap of
       * all of the items in the tag.
       *
       * This is the most powerful structure for accessing the items of the tag.
       *
       * APE tags are case-insensitive, all keys in this map have been converted
       * to upper case.
       *
       * \warning You should not modify this data structure directly, instead
       * use setItem() and removeItem().
       */
      const ItemListMap &itemListMap() const;

      /*!
       * Removes the \a key item from the tag
       */
      void removeItem(const String &key);

      /*!
       * Adds to the text item specified by \a key the data \a value.  If \a replace
       * is true, then all of the other values on the same key will be removed
       * first.  If a binary item exists for \a key it will be removed first.
       */
      void addValue(const String &key, const String &value, bool replace = true);

     /*!
      * Set the binary data for the key specified by \a item to \a value
      * This will convert the item to type \a Binary if it isn't already and
      * all of the other values on the same key will be removed.
      */
      void setData(const String &key, const ByteVector &value);

      /*!
       * Sets the \a key item to the value of \a item. If an item with the \a key is already
       * present, it will be replaced.
       */
      void setItem(const String &key, const Item &item);

      /*!
       * Returns true if the tag does not contain any data.
       */
      bool isEmpty() const override;

    protected:

      /*!
       * Reads from the file specified in the constructor.
       */
      void read();

      /*!
       * Parses the body of the tag in \a data.
       */
      void parse(const ByteVector &data);

    private:
      class TagPrivate;
      TAGLIB_MSVC_SUPPRESS_WARNING_NEEDS_TO_HAVE_DLL_INTERFACE
      std::unique_ptr<TagPrivate> d;
    };
  }  // namespace APE
}  // namespace TagLib

#endif
