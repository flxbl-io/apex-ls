/*
 [The "BSD licence"]
 Copyright (c) 2020 Kevin Jones
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.nawforce.common.org.stream

import com.nawforce.common.api.Name
import com.nawforce.common.diagnostics.IssueLogger
import com.nawforce.common.documents.{DocumentIndex, LineLocationImpl, MetadataDocument}

case class MetadataDocumentWithData(docType: MetadataDocument, data: String)

/** Provider for metadata files */
trait MetadataProvider {
  /** Retrieves files of a specific type, if file is not readable an issue is logged. */
  def retrieve(metadataExt: Name, logger: IssueLogger): Set[MetadataDocumentWithData]
}

/** MetadataProvider that uses a DocumentIndex as a source */
class DocumentIndexMetadataProvider(index: DocumentIndex) extends MetadataProvider {

  /** Retrieve from the provided DocumentIndex with error handling */
  override def retrieve(metadataExt: Name, logger: IssueLogger): Set[MetadataDocumentWithData] = {
    index.getByExtension(metadataExt).flatMap(documentType => {
      if (!documentType.path.isFile) {
        logger.logError(LineLocationImpl(documentType.path.toString, 0),
          s"Expecting metadata to be in a regular file")
        None
      } else {
        documentType.path.read() match {
          case Left(err) =>
            logger.logError(LineLocationImpl(documentType.path.toString, 0),
              s"Could not read file ${documentType.path}, error '$err")
            None
          case Right(data) =>
            Some(MetadataDocumentWithData(documentType, data))
        }
      }
    })
  }
}

/** Override MetadataProvider for injecting new metadata not stored in a file. */
class OverrideMetadataProvider(overrides: Seq[MetadataDocumentWithData], base: MetadataProvider)
  extends MetadataProvider {

  private lazy val overrideByExt: Map[Name, Seq[MetadataDocumentWithData]] = overrides.groupBy(_.docType.extension)

  override def retrieve(metadataExt: Name, logger: IssueLogger): Set[MetadataDocumentWithData] = {
    val metadata = base.retrieve(metadataExt, logger)
    val overrides = overrideByExt.getOrElse(metadataExt, Seq())
    val overridesByPath = overrides.groupBy(_.docType.path)

    metadata.filterNot(md => overridesByPath.contains(md.docType.path)) ++ overrides
  }
}