package main

import "C"

import (
	"encoding/json"
	"gopkg.in/bblfsh/client-go.v3"
)

//export Parse
func Parse(text string, language string, endpoint string) *C.char {
	client, err := bblfsh.NewClient(endpoint)
	if err != nil {
		return C.CString("ERROR-0" + err.Error())
	}

	res, _, err := client.NewParseRequest().Content(text).Language(language).UAST()
	if err != nil {
		return C.CString("ERROR-1" + err.Error())
	}

	data, err := json.MarshalIndent(res, "", "  ")
    if err != nil {
        return C.CString("ERROR-2" + err.Error())
    }

	return C.CString(string(data))
}

func main() {
}