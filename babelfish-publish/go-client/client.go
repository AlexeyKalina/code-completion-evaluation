package main

import "C"

import (
	"encoding/json"
	"gopkg.in/bblfsh/client-go.v3"
)

//export Parse
func Parse(file string, endpoint string) *C.char {
	client, err := bblfsh.NewClient(endpoint)
	if err != nil {
		panic(err)
	}

	res, _, err := client.NewParseRequest().ReadFile(file).UAST()
	if err != nil {
		panic(err)
	}

	data, err := json.MarshalIndent(res, "", "  ")
	if err != nil {
		panic(err)
	}

	return C.CString(string(data))
}

func main() {
}