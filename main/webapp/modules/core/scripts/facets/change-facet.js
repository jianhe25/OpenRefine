/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

function ChangeFacet(div, config, options, selection) {
  this._div = div;
  this._config = config;
  if (!("invert" in this._config)) {
    this._config.invert = false;
  }

  this._options = options || {};
  //if (!("sort" in this._options)) {
  //  this._options.sort = "name";
  //}

  this._selection = selection || [];
  this._blankChoice = (config.selectBlank) ? { s : true, c : 0 } : null;
  this._errorChoice = (config.selectError) ? { s : true, c : 0 } : null;

  this._data = null;

  this._initializeUI();
  this._update();
}

ChangeFacet.reconstruct = function(div, uiState) {
  return new ChangeFacet(div, uiState.c, uiState.o, uiState.s);
};

ChangeFacet.prototype.dispose = function() {
};

ChangeFacet.prototype.reset = function() {
  this._selection = [];
  this._blankChoice = null;
  this._errorChoice = null;
};

ChangeFacet.prototype.getUIState = function() {
  var json = {
      c: this.getJSON(),
      o: this._options
  };

  json.s = json.c.selection;
  delete json.c.selection;

  return json;
};

ChangeFacet.prototype.getJSON = function() {
  var o = {
      type: "recommendChange",
      name: this._config.name,
      columnName: this._config.columnName,
      expression: this._config.expression,
      omitBlank: "omitBlank" in this._config ? this._config.omitBlank : false,
          omitError: "omitError" in this._config ? this._config.omitError : false,
              selection: [],
              selectBlank: this._blankChoice !== null && this._blankChoice.s,
              selectError: this._errorChoice !== null && this._errorChoice.s,
              invert: this._config.invert,
      rowIndex: this._config.rowIndex,
      columnIndex: this._config.columnIndex,
      from: this._config.from,
      to: this._config.to,
      valueType: this._config.type,
      historyChoices: [],
  };
  for (var i = 0; i < this._selection.length; i++) {
    var choice = {
        v: cloneDeep(this._selection[i].v)
    };
    o.selection.push(choice);
  }
  if (sessionStorage.getItem("historyChoices")) {
    var historyChoices = JSON.parse(sessionStorage.getItem("historyChoices"));
    for (var i = 0; i < historyChoices.length; i++) {
      o.historyChoices.push(historyChoices[i]);
    }
  }
  return o;
};

ChangeFacet.prototype.hasSelection = function() {
  return this._selection.length > 0 || 
  (this._blankChoice !== null && this._blankChoice.s) || 
  (this._errorChoice !== null && this._errorChoice.s);
};

ChangeFacet.prototype.updateState = function(data) {
  this._data = data;

  if ("choices" in data) {
    var selection = [];
    var choices = data.choices;
    for (var i = 0; i < choices.length; i++) {
      var choice = choices[i];
      if (choice.s) {
        selection.push(choice);
      }
    }
    this._selection = selection;
    this._reSortChoices();

    this._blankChoice = data.blankChoice || null;
    this._errorChoice = data.errorChoice || null;
  }

  this._update();
};

ChangeFacet.prototype._reSortChoices = function() {
  this._data.choices.sort(this._options.sort == "name" ?
      function(a, b) {
    return a.v.l.toLowerCase().localeCompare(b.v.l.toLowerCase());
  } :
    function(a, b) {
    var c = b.c - a.c;
    return c !== 0 ? c : a.v.l.localeCompare(b.v.l);
  }
  );
};

ChangeFacet.prototype._initializeUI = function() {
  var self = this;

  var facet_id = this._div.attr("id");

  this._div.empty().show().html(
      '<div class="facet-title">' +
        '<div class="grid-layout layout-tightest layout-full"><table><tr>' +
          '<td width="1%"><a href="javascript:{}" title="'+$.i18n._('core-facets')["remove-facet"]+'" class="facet-title-remove" bind="removeButton">&nbsp;</a></td>' +
          '<td>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">'+$.i18n._('core-facets')["reset"]+'</a>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="invertButton">'+$.i18n._('core-facets')["invert"]+'</a>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="changeButton">'+$.i18n._('core-facets')["change"]+'</a>' +
            '<span bind="titleSpan"></span>' +
          '</td>' +
        '</tr></table></div>' +
      '</div>' +
      '<div class="facet-expression" bind="expressionDiv" title="'+$.i18n._('core-facets')["click-to-edit"]+'"></div>' +
      '<div class="facet-controls" bind="controlsDiv" style="display:none;">' +
        '<a bind="choiceCountContainer" class="action" href="javascript:{}"></a> ' +
        '<span class="facet-controls-sortControls" bind="sortGroup">'+$.i18n._('core-facets')["sort-by"]+': ' +
          '<a href="javascript:{}" bind="sortByNameLink">'+$.i18n._('core-facets')["name"]+'</a>' +
          '<a href="javascript:{}" bind="sortByCountLink">'+$.i18n._('core-facets')["count"]+'</a>' +
        '</span>' +
      '<button bind="applyLink" class="facet-controls-button button" style="float: right; margin-left: 17px;">'+'Apply'+'</button>' +
      '<div style="clear:both"> </div>' +
      '</div>' +
      '<div class="facet-body" bind="bodyDiv">' +
        '<div class="facet-body-inner" bind="bodyInnerDiv"></div>' +
      '</div>'
  );
  this._elmts = DOM.bind(this._div);

  this._elmts.titleSpan.text("Change " + this._config.from + " to " + this._config.to + " when");
  this._elmts.changeButton.attr("title",$.i18n._('core-facets')["current-exp"]+": " + this._config.expression).click(function() {
    self._elmts.expressionDiv.slideToggle(100, function() {
      if (self._elmts.expressionDiv.css("display") != "none") {
        self._editExpression();
      }
    });
  });
  this._elmts.expressionDiv.text(this._config.expression).hide().click(function() { self._editExpression(); });
  this._elmts.removeButton.click(function() { self._remove(); });
  this._elmts.resetButton.click(function() { self._reset(); });
  this._elmts.invertButton.click(function() { self._invert(); });

  this._elmts.choiceCountContainer.click(function() { self._copyChoices(); });
  this._elmts.sortByCountLink.click(function() {
    if (self._options.sort != "count") {
      self._options.sort = "count";
      self._reSortChoices();
      self._update(true);
    }
  });
  this._elmts.sortByNameLink.click(function() {
    if (self._options.sort != "name") {
      self._options.sort = "name";
      self._reSortChoices();
      self._update(true);
    }
  });

  this._elmts.applyLink.click(function() {
    var historyChoices = JSON.parse(sessionStorage.getItem("historyChoices"));
    if (historyChoices == null)
      historyChoices = self._data.choices;
    if (historyChoices[historyChoices.length - 1] != self._data.choices[self._data.choices.length - 1])
      historyChoices = historyChoices.concat(self._data.choices);
    sessionStorage.clear();
    sessionStorage.setItem("historyChoices", JSON.stringify(historyChoices));
    commitChange();
    if ($(this).text() == "Finish") {
      self._remove();
    }
    Refine.update({ engineChanged: true });
  });
  //if (this._config.expression != "value" && this._config.expression != "grel:value") {
  //  this._elmts.clusterLink.hide();
  //}

  if (!("scroll" in this._options) || this._options.scroll) {
    this._elmts.bodyDiv.addClass("facet-body-scrollable");
    this._elmts.bodyDiv.resizable({
      minHeight: 30,
      handles: 's',
      stop: function(event, ui) {
        event.target.style.width = "auto"; // don't force the width
      }
    });
  }

  var commitChange = function() {
    //console.log("come to commit change");
    var choices = self._data.choices;
    var correctChoices = []
    for (var i = 0; i < choices.length; ++i) {
      if (choices[i].r) {
        correctChoices.push(choices[i]);
      }
    }
    if (correctChoices.length == 0)
      return;
    Refine.postCoreProcess(
        "recommended-edit",
        {},
        {
          columnName: self._config.columnName,
          expression: "value",
          from: self._config.from,
          to: self._config.to,
          correctChoices: JSON.stringify(correctChoices)
        },
        {
          // limit edits to rows constrained only by the other facets
          engineConfig: ui.browsingEngine.getJSON(false, self),
          cellsChanged: true
        },
        {
          onDone: function(o) {
            var selection = [];
            var gotSelection = false;
            for (var i = 0; i < self._selection.length; i++) {
              var choice = self._selection[i];
              if (choice.v.v == originalContent) {
                if (gotSelection) {
                  continue;
                }
                choice.v.v = text;
                gotSelection = true; // eliminate duplicated selections due to changing one selected choice to another
              }
              selection.push(choice);
            }
            self._selection = selection;
          }
        }
    );
  };
};

ChangeFacet.prototype._copyChoices = function() {
  var self = this;
  var frame = DialogSystem.createDialog();
  frame.width("600px");

  var header = $('<div></div>').addClass("dialog-header").text($.i18n._('core-facets')["facet-choices"]).appendTo(frame);
  var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
  var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

  body.html('<textarea wrap="off" bind="textarea" style="display: block; width: 100%; height: 400px;" />');
  var elmts = DOM.bind(body);

  $('<button class="button"></button>').text($.i18n._('core-buttons')["close"]).click(function() {
    DialogSystem.dismissUntil(level - 1);
  }).appendTo(footer);

  var lines = [];
  for (var i = 0; i < this._data.choices.length; i++) {
    var choice = this._data.choices[i];
    lines.push(choice.v.l + "\t" + choice.c);
  }
  if (this._blankChoice) {
    lines.push("(blank)\t" + this._blankChoice.c);
  }
  if (this._errorChoice) {
    lines.push("(error)\t" + this._errorChoice.c);
  }

  var level = DialogSystem.showDialog(frame);

  var textarea = elmts.textarea[0];
  textarea.value = lines.join("\n");
  textarea.focus();
  textarea.select();
};

ChangeFacet.prototype._update = function(resetScroll) {
  var self = this;

  var invert = this._config.invert;
  if (invert) {
    this._elmts.bodyInnerDiv.addClass("facet-mode-inverted");
    this._elmts.invertButton.addClass("facet-mode-inverted");
  } else {
    this._elmts.bodyInnerDiv.removeClass("facet-mode-inverted");
    this._elmts.invertButton.removeClass("facet-mode-inverted");
  }

  if (!this._data) {
    //this._elmts.statusDiv.hide();
    this._elmts.controlsDiv.hide();
    this._elmts.bodyInnerDiv.empty().append(
        $('<div>').text($.i18n._('core-facets')["loading"]).addClass("facet-body-message"));

    return;
  } else if ("error" in this._data) {
    //this._elmts.statusDiv.hide();
    this._elmts.controlsDiv.hide();

    if (this._data.error == "Too many choices") {
      this._elmts.bodyInnerDiv.empty();
      
      var messageDiv = $('<div>')
        .text(this._data.choiceCount + " "+$.i18n._('core-facets')["too-many-choices"])
        .addClass("facet-body-message")
        .appendTo(this._elmts.bodyInnerDiv);
      $('<br>').appendTo(messageDiv);
      $('<a>')
      .text($.i18n._('core-facets')["set-choice-count"])
      .attr("href", "javascript:{}")
      .addClass("action")
      .addClass("secondary")
      .appendTo(messageDiv)
      .click(function() {
        self._setChoiceCountLimit(self._data.choiceCount);
      });
      
      this._renderBodyControls();
    } else {
      this._elmts.bodyInnerDiv.empty().append(
          $('<div>')
            .text(this._data.error)
            .addClass("facet-body-message"));
    }
    return;
  }

  var scrollTop = 0;
  if (!resetScroll) {
    try {
      scrollTop = this._elmts.bodyInnerDiv[0].scrollTop;
    } catch (e) {
    }
  }

  // FIXME: this is very slow for large numbers of choices (e.g. 18 seconds for 13K choices)
  this._elmts.bodyInnerDiv.empty();
  
  // None of the following alternatives are significantly faster
  
//  this._elmts.bodyInnerDiv.innerHtml = '';
  
//  this._elmts.bodyInnerDiv.detach();
//  this._elmts.bodyInnerDiv.children().remove();
//  this._elmts.bodyInnerDiv.appendTo('.facet-body');
  
//  this._elmts.bodyInnerDiv.remove();
//  this._elmts.bodyInnerDiv.html('<div class="facet-body-inner" bind="bodyInnerDiv"></div>');
//  this._elmts.bodyInnerDiv.appendTo('.facet-body');

  //this._elmts.statusDiv.show();
  this._elmts.controlsDiv.show();

  if (this._data.isLastBatch) {
    $(".facet-controls-button").text("Finish");
  }

  var choices = this._data.choices;
  var selectionCount = this._selection.length +
  (this._blankChoice !== null && this._blankChoice.s ? 1 : 0) +
  (this._errorChoice !== null && this._errorChoice.s ? 1 : 0);

  this._elmts.choiceCountContainer.text(choices.length + " choices");
  if (selectionCount > 0) {
    this._elmts.resetButton.show();
    this._elmts.invertButton.show();
  } else {
    this._elmts.resetButton.hide();
    this._elmts.invertButton.hide();
  }

  if (this._options.sort == "name") {
    this._elmts.sortByNameLink.removeClass("action").addClass("selected");
    this._elmts.sortByCountLink.removeClass("selected").addClass("action");
  } else {
    this._elmts.sortByNameLink.removeClass("selected").addClass("action");
    this._elmts.sortByCountLink.removeClass("action").addClass("selected");
  }

  var html = [];
  var temp = $('<div>');
  var encodeHtml = function(s) {
    return temp.text(s).html();
  };


  var renderEdit = this._config.expression == "value";
  var renderChoice = function(index, choice, customLabel) {
    var label = customLabel || choice.v.l;
    var count = choice.c;
    html.push('<div class="facet-choice' + (choice.s ? ' facet-choice-selected' : '') + '" choiceIndex="' + index + '">');

    // include/exclude link
    //html.push(
    //  '<a href="javascript:{}" class="facet-choice-link facet-choice-toggle" ' +
    //    'style="visibility: ' + (choice.s ? 'visible' : 'hidden') + '">' +
    //    (invert != choice.s ? 'exclude' : 'include') +
    //  '</a>'
    //);

    // edit link
    //if (renderEdit) {
    //  html.push('<a href="javascript:{}" class="facet-choice-link facet-choice-edit" style="visibility: hidden">'+$.i18n._('core-facets')["edit"]+'</a>');
    //}


    var isChecked = false
    if (sessionStorage.getItem("checkbox-" + index)) {
      // Restore the contents of the text field
      isChecked = sessionStorage.getItem("checkbox-" + index);
    }
    if (isChecked)
      html.push('<input type="checkbox" ' + ' choiceIndex=' + index + ' checked>')
    else
      html.push('<input type="checkbox" ' + ' choiceIndex=' + index + '>')
    //html.push('<button style="margin-right:10px">x</button>')
    //html.push('<span class="glyphicon glyphicon-ok"></span>')
    //html.push('<span class="glyphicon glyphicon-remove" style="margin-left: 5px"></span>')
    html.push('<a href="javascript:{}" class="facet-choice-label" style="margin-left: 5px">' + encodeHtml(label) + '</a>');
    html.push('<span class="facet-choice-count">' + (invert ? "-" : "") + count + '</span>');
    html.push('</div>');
  };
  for (var i = 0; i < choices.length; i++) {
    renderChoice(i, choices[i]);
  }
  if (this._blankChoice !== null) {
    renderChoice(-1, this._blankChoice, "(blank)");
  }
  if (this._errorChoice !== null) {
    renderChoice(-2, this._errorChoice, "(error)");
  }

  this._elmts.bodyInnerDiv.html(html.join(''));
  this._renderBodyControls();
  this._elmts.bodyInnerDiv[0].scrollTop = scrollTop;



  var getChoice = function(elmt) {
    var index = parseInt(elmt.attr("choiceIndex"),10);
    if (index == -1) {
      return self._blankChoice;
    } else if (index == -2) {
      return self._errorChoice;
    } else {
      return choices[index];
    }
  };
  var findChoice = function(elmt) {
    return getChoice(elmt.closest('.facet-choice'));
  };
  var select = function(choice) {
    self._select(choice, false);
  };
  var selectOnly = function(choice) {
    self._select(choice, true);
  };
  var deselect = function(choice) {
    self._deselect(choice);
  };

  $('[type="checkbox"]').change(function() {
    var index = parseInt($(this).attr("choiceIndex"), 10);
    sessionStorage.setItem("checkbox-" + index, $(this).is(":checked"));
    self._data.choices[index].r = $(this).is(":checked");
  })

  var wireEvents = function() {
    var bodyInnerDiv = self._elmts.bodyInnerDiv;
    bodyInnerDiv.off(); // remove all old handlers
    bodyInnerDiv.on('click', '.facet-choice-label', function(e) {
      e.preventDefault();
      var choice = findChoice($(this));
      var highlightColumnIndexs = JSON.stringify($.map(choice.v.predicates, function (predicate) { return predicate.c} ));
      sessionStorage.setItem("highlightColumns", highlightColumnIndexs);
      sessionStorage.setItem("changeColumn", parseInt(self._config.columnIndex));
      //console.log('self.columnIndex', self._config.columnIndex);
      DataTableView.highlightColumnsByFacetSelection(highlightColumnIndexs, self.columnIndex);
      if (choice.s) {
        if (selectionCount > 1) {
          selectOnly(choice);
        } else {
          deselect(choice);
        }
      } else if (selectionCount > 0) {
        selectOnly(choice);
      } else {
        select(choice);
      }
    });



    bodyInnerDiv.on('click', '.facet-choice-edit', function(e) {
      e.preventDefault();
      var choice = findChoice($(this));
      self._editChoice(choice, $(this).closest('.facet-choice'));
    });

    bodyInnerDiv.on('mouseenter mouseleave', '.facet-choice', function(e) {
      e.preventDefault();
      var visibility = 'visible';
      if (e.type == 'mouseleave') {
        visibility = 'hidden';
      }
      $(this).find('.facet-choice-edit').css("visibility", visibility);

      var choice = getChoice($(this));
      if (!choice.s) {
        $(this).find('.facet-choice-toggle').css("visibility", visibility);
      }
    });

    bodyInnerDiv.on('click', '.facet-choice-toggle', function(e) {
      e.preventDefault();
      var choice = findChoice($(this));
      if (choice.s) {
        deselect(choice);
      } else {
        select(choice);
      }
    });
  };
  wireEvents();

  if (this._selection.length == 0) {
    $('.facet-choice-label').first().click();
  }
}; // end _update()

ChangeFacet.prototype._renderBodyControls = function() {
  var self = this;
  var bodyControls = $('<div>')
  .addClass("facet-body-controls")
  .appendTo(this._elmts.bodyInnerDiv);

  $('<a>')
  .text($.i18n._('core-facets')["facet-by-count"])
  .attr("href", "javascript:{}")
  .addClass("action")
  .addClass("secondary")
  .appendTo(bodyControls)
  .click(function() {
    ui.browsingEngine.addFacet(
      "range", 
      {
        "name" : self._config.columnName,
        "columnName" : self._config.columnName, 
        "expression" : self._getMetaExpression(),
        "mode" : "range"
      },
      {
      }
    );
  });
};

ChangeFacet.prototype._getMetaExpression = function() {
  var r = Scripting.parse(this._config.expression);

  return r.language + ':facetCount(' + [
        r.expression,
        JSON.stringify(this._config.expression),
        JSON.stringify(this._config.columnName)
      ].join(', ') + ')';
};

ChangeFacet.prototype._doEdit = function() {
  new ClusteringDialog(this._config.columnName, this._config.expression);
};

ChangeFacet.prototype._editChoice = function(choice, choiceDiv) {
  var self = this;

  var menu = MenuSystem.createMenu().addClass("data-table-cell-editor").width("400px");
  menu.html(
      '<textarea class="data-table-cell-editor-editor" bind="textarea" />' +
      '<div id="data-table-cell-editor-actions">' +
        '<div class="data-table-cell-editor-action">' +
          '<button class="button" bind="okButton">'+$.i18n._('core-buttons')["apply"]+'</button>' +
          '<div class="data-table-cell-editor-key">'+$.i18n._('core-buttons')["enter"]+'</div>' +
        '</div>' +
        '<div class="data-table-cell-editor-action">' +
          '<button class="button" bind="cancelButton">'+$.i18n._('core-buttons')["cancel"]+'</button>' +
          '<div class="data-table-cell-editor-key">'+$.i18n._('core-buttons')["esc"]+'</div>' +
        '</div>' +
      '</div>'
  );
  var elmts = DOM.bind(menu);

  MenuSystem.showMenu(menu, function(){});
  MenuSystem.positionMenuLeftRight(menu, choiceDiv);

  var originalContent;
  if (choice === this._blankChoice) {
    originalContent = "(blank)";
  } else if (choice === this._errorChoice) {
    originalContent = "(error)";
  } else {
    originalContent = choice.v.v;
  }

  var commit = function() {
    var text = elmts.textarea[0].value;

    MenuSystem.dismissAll();

    var edit = { to : text };
    if (choice === self._blankChoice) {
      edit.fromBlank = true;
    } else if (choice === self._errorChoice) {
      edit.fromError = true;
    } else {
      edit.from = [ originalContent ];
    }

    Refine.postCoreProcess(
      "mass-edit",
      {},
      {
        columnName: self._config.columnName,
        expression: "value",
        edits: JSON.stringify([ edit ])
      },
      {
        // limit edits to rows constrained only by the other facets
        engineConfig: ui.browsingEngine.getJSON(false, self),
        cellsChanged: true
      },
      {
        onDone: function(o) {
          var selection = [];
          var gotSelection = false;
          for (var i = 0; i < self._selection.length; i++) {
            var choice = self._selection[i];
            if (choice.v.v == originalContent) {
              if (gotSelection) {
                continue;
              }
              choice.v.v = text;
              gotSelection = true; // eliminate duplicated selections due to changing one selected choice to another
            }
            selection.push(choice);
          }
          self._selection = selection;
        }
      }
    );            
  };

  elmts.okButton.click(commit);
  elmts.textarea
  .text(originalContent)
  .keydown(function(evt) {
    if (!evt.shiftKey) {
      if (evt.keyCode == 13) {
        commit();
      } else if (evt.keyCode == 27) {
        MenuSystem.dismissAll();
      }
    }
  })
  .select()
  .focus();

  elmts.cancelButton.click(function() {
    MenuSystem.dismissAll();
  });
};

ChangeFacet.prototype._select = function(choice, only) {
  if (only) {
    this._selection = [];
    if (this._blankChoice !== null) {
      this._blankChoice.s = false;
    }
    if (this._errorChoice !== null) {
      this._errorChoice.s = false;
    }
  }

  choice.s = true;
  if (choice !== this._errorChoice && choice !== this._blankChoice) {
    this._selection.push(choice);
  }

  this._updateRest();
};

ChangeFacet.prototype._deselect = function(choice) {
  if (choice === this._errorChoice || choice === this._blankChoice) {
    choice.s = false;
  } else {
    for (var i = this._selection.length - 1; i >= 0; i--) {
      if (this._selection[i] === choice) {
        this._selection.splice(i, 1);
        break;
      }
    }
  }
  this._updateRest();
};

ChangeFacet.prototype._reset = function() {
  this._selection = [];
  this._blankChoice = null;
  this._errorChoice = null;
  this._config.invert = false;

  this._updateRest();
};

ChangeFacet.prototype._invert = function() {
  this._config.invert = !this._config.invert;

  this._updateRest();
};

ChangeFacet.prototype._remove = function() {
  ui.browsingEngine.removeFacet(this);

  this._div = null;
  this._config = null;

  this._selection = null;
  this._blankChoice = null;
  this._errorChoice = null;
  this._data = null;
  sessionStorage.clear();
};

ChangeFacet.prototype._updateRest = function() {
  Refine.update({ engineChanged: true });
};

ChangeFacet.prototype._editExpression = function() {
  var self = this;
  var title = (this._config.columnName) ? 
      ($.i18n._('core-facets')["edit-based-col"]+" " + this._config.columnName) : 
    	  $.i18n._('core-facets')["edit-facet-exp"];

  var column = Refine.columnNameToColumn(this._config.columnName);
  var o = DataTableView.sampleVisibleRows(column);

  new ExpressionPreviewDialog(
    title,
    column ? column.cellIndex : -1, 
    o.rowIndices,
    o.values,
    this._config.expression, 
    function(expr) {
      if (expr != self._config.expression) {
        self._config.expression = expr;

        self._elmts.expressionDiv.text(self._config.expression);
        self._elmts.changeButton.attr("title", $.i18n._('core-facets')["current-exp"]+": " + self._config.expression);
        //if (self._config.expression == "value" || self._config.expression == "grel:value") {
        //  self._elmts.clusterLink.show();
        //} else {
        //  self._elmts.clusterLink.hide();
        //}

        self.reset();
        self._updateRest();
      }
      self._elmts.expressionDiv.hide();
    }
  );
};

ChangeFacet.prototype._setChoiceCountLimit = function(choiceCount) {
  var limit = Math.ceil(choiceCount / 1000) * 1000;
  var s = window.prompt($.i18n._('core-facets')["set-max-choices"], limit);
  if (s) {
    var n = parseInt(s,10);

    if (!isNaN(n)) {
      var self = this;
      $.post(
        "command/core/set-preference",
        {
          name : "ui.browsing.ChangeFacet.limit",
          value : n
        },
        function(o) {
          if (o.code == "ok") {
            ui.browsingEngine.update();
          } else if (o.code == "error") {
            alert(o.message);
          }
        },
        "json"
      );      
    }
  }
};
